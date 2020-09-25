/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.continuationParameter
import org.jetbrains.kotlin.backend.jvm.codegen.hasContinuation
import org.jetbrains.kotlin.backend.jvm.codegen.isInvokeSuspendOfContinuation
import org.jetbrains.kotlin.backend.jvm.codegen.isReadOfCrossinline
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.defaultValue
import org.jetbrains.kotlin.backend.jvm.ir.isStaticInlineClassReplacement
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.Type

internal val suspendLambdaPhase = makeIrFilePhase(
    ::SuspendLambdaLowering,
    "SuspendLambda",
    "Transform suspend lambdas into continuation classes"
)

internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation classes and parameters to suspend functions",
    prerequisite = setOf(suspendLambdaPhase, localDeclarationsPhase, tailCallOptimizationPhase)
)

private abstract class SuspendLoweringBase(protected val context: JvmBackendContext) : FileLoweringPass {
    protected fun IrClass.addFunctionOverride(function: IrSimpleFunction): IrSimpleFunction =
        addFunction(function.name.asString(), function.returnType).apply {
            overriddenSymbols += function.symbol
            valueParameters = function.valueParameters.map { it.copyTo(this) }
        }

    protected fun IrClass.addFunctionOverride(
        function: IrSimpleFunction,
        makeBody: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrSimpleFunction =
        addFunctionOverride(function).apply {
            body = context.createIrBuilder(symbol).irBlockBody { makeBody(this@apply) }
        }

    protected fun IrSimpleFunction.generateErrorForInlineBody() {
        val message = "This is a stub representing a copy of a suspend method without the state machine " +
                "(used by the inliner). Since the difference is at the bytecode level, the body is " +
                "still on the original function. Use suspendForInlineToOriginal() to retrieve it."
        body = IrExpressionBodyImpl(startOffset, endOffset, IrErrorExpressionImpl(startOffset, endOffset, returnType, message))
    }

    protected fun IrFunction.addCompletionValueParameter(): IrValueParameter =
        addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType())

    protected fun IrFunction.continuationType(): IrType =
        context.ir.symbols.continuationClass.typeWith(returnType).makeNullable()
}

private fun IrFunction.capturesCrossinline(): Boolean {
    var result = false
    accept(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (!result) element.acceptChildren(this, null)
        }

        override fun visitFunction(declaration: IrFunction) {
            functions.add(declaration)
            super.visitFunction(declaration)
            functions.remove(declaration)
        }

        override fun visitGetValue(expression: IrGetValue) {
            result = result || (expression.isReadOfCrossinline() && expression.symbol.owner.parent !in functions)
        }

        private val functions = mutableSetOf<IrFunction>()
    }, null)
    return result
}

private class SuspendLambdaLowering(context: JvmBackendContext) : SuspendLoweringBase(context) {
    override fun lower(irFile: IrFile) {
        val inlineReferences = IrInlineReferenceLocator.scan(context, irFile)
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            private fun IrFunctionReference.shouldBeTreatedAsSuspendLambda() =
                isSuspend && (origin == IrStatementOrigin.LAMBDA || origin == IrStatementOrigin.SUSPEND_CONVERSION)

            override fun visitBlock(expression: IrBlock): IrExpression {
                val reference = expression.statements.lastOrNull() as? IrFunctionReference ?: return super.visitBlock(expression)
                if (reference.shouldBeTreatedAsSuspendLambda() && reference !in inlineReferences) {
                    assert(expression.statements.size == 2 && expression.statements[0] is IrFunction)
                    expression.transformChildrenVoid(this)
                    val parent = currentDeclarationParent ?: error("No current declaration parent at ${reference.dump()}")
                    return generateAnonymousObjectForLambda(reference, parent)
                }
                return super.visitBlock(expression)
            }
        })
    }

    private fun generateAnonymousObjectForLambda(reference: IrFunctionReference, parent: IrDeclarationParent) =
        context.createIrBuilder(reference.symbol).irBlock(reference.startOffset, reference.endOffset) {
            assert(reference.getArguments().isEmpty()) { "lambda with bound arguments: ${reference.render()}" }
            val continuation = generateContinuationClassForLambda(reference, parent)
            +continuation
            +irCall(continuation.constructors.single().symbol).apply {
                // Pass null as completion parameter
                putValueArgument(0, irNull())
            }
        }

    private fun generateContinuationClassForLambda(reference: IrFunctionReference, parent: IrDeclarationParent): IrClass =
        context.irFactory.buildClass {
            name = SpecialNames.NO_NAME_PROVIDED
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            this.parent = parent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(reference)

            val function = reference.symbol.owner
            val isRestricted = reference.symbol.owner.extensionReceiverParameter?.type?.classOrNull?.owner?.annotations?.any {
                it.type.classOrNull?.signature == IdSignature.PublicSignature("kotlin.coroutines", "RestrictsSuspension", null, 0)
            } == true
            val suspendLambda =
                if (isRestricted) context.ir.symbols.restrictedSuspendLambdaClass.owner
                else context.ir.symbols.suspendLambdaClass.owner
            val arity = (reference.type as IrSimpleType).arguments.size - 1
            val functionNClass = context.ir.symbols.getJvmFunctionClass(arity + 1)
            superTypes += suspendLambda.defaultType
            superTypes += functionNClass.typeWith(
                function.explicitParameters.subList(0, arity).map { it.type }
                        + function.continuationType()
                        + context.irBuiltIns.anyNType
            )

            addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaDescriptorVisibilities.PACKAGE_VISIBILITY)

            val parametersFields = function.explicitParameters.map {
                addField {
                    // Rename `$this` to avoid being caught by inlineCodegenUtils.isCapturedFieldName()
                    name = if (it.index < 0) Name.identifier("p\$") else it.name
                    type = it.type
                    origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                    isFinal = false
                    visibility = if (it.index < 0) DescriptorVisibilities.PRIVATE else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                }
            }
            val constructor = addPrimaryConstructorForLambda(suspendLambda, arity)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.valueParameters.size == arity + 1 && it.owner.name.asString() == "invoke"
            }
            val createToOverride = suspendLambda.symbol.functions.singleOrNull {
                it.owner.valueParameters.size == arity + 1 && it.owner.name.asString() == "create"
            }
            val invokeSuspend = addInvokeSuspendForLambda(function, parametersFields)
            if (function.capturesCrossinline()) {
                addInvokeSuspendForInlineForLambda(invokeSuspend)
            }
            if (createToOverride != null) {
                addInvokeCallingCreate(addCreate(constructor, createToOverride, parametersFields), invokeSuspend, invokeToOverride)
            } else {
                addInvokeCallingConstructor(constructor, invokeSuspend, invokeToOverride, parametersFields)
            }

            context.suspendLambdaToOriginalFunctionMap[attributeOwnerId as IrFunctionReference] = function
        }

    private fun IrClass.addInvokeSuspendForLambda(irFunction: IrFunction, fields: List<IrField>): IrSimpleFunction {
        val superMethod = context.ir.symbols.suspendLambdaClass.functions.single {
            it.owner.name.asString() == INVOKE_SUSPEND_METHOD_NAME && it.owner.valueParameters.size == 1 &&
                    it.owner.valueParameters[0].type.isKotlinResult()
        }.owner
        return addFunctionOverride(superMethod).apply {
            body = irFunction.moveBodyTo(this, mapOf())?.transform(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    val parameter = (expression.symbol.owner as? IrValueParameter)?.takeIf { it.parent == irFunction }
                        ?: return expression
                    val field = fields[parameter.index + if (irFunction.extensionReceiverParameter != null) 1 else 0]
                    val receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter!!.symbol)
                    return IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, field.type, receiver)
                }
            }, null)
        }
    }

    private fun IrClass.addInvokeSuspendForInlineForLambda(invokeSuspend: IrSimpleFunction): IrSimpleFunction {
        return addFunction(
            INVOKE_SUSPEND_METHOD_NAME + FOR_INLINE_SUFFIX,
            context.irBuiltIns.anyNType,
            Modality.FINAL,
            origin = JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
        ).apply {
            copyAttributes(invokeSuspend)
            generateErrorForInlineBody()
            valueParameters = invokeSuspend.valueParameters.map { it.copyTo(this) }
        }
    }

    // Invoke function in lambdas is responsible for
    //   1) calling `create`
    //   2) starting newly created coroutine by calling `invokeSuspend`.
    // Thus, it creates a clone of suspend lambda and starts it.
    // TODO: fix the generic signature -- type parameters of FunctionN should be substituted
    private fun IrClass.addInvokeCallingCreate(
        create: IrFunction,
        invokeSuspend: IrSimpleFunction,
        invokeToOverride: IrSimpleFunctionSymbol
    ) = addFunctionOverride(invokeToOverride.owner) { function ->
        val newlyCreatedObject = irCall(create).also { createCall ->
            createCall.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
            for ((index, param) in function.valueParameters.withIndex()) {
                createCall.putValueArgument(index, irGet(param))
            }
        }
        +irReturn(callInvokeSuspend(invokeSuspend, irImplicitCast(newlyCreatedObject, defaultType)))
    }

    // Same as above, but with `create` inlined. `create` is only defined in `SuspendLambda` in unary and binary
    // versions; for other lambdas, there's no point in generating a non-overriding `create` separately.
    private fun IrClass.addInvokeCallingConstructor(
        constructor: IrFunction,
        invokeSuspend: IrSimpleFunction,
        invokeToOverride: IrSimpleFunctionSymbol,
        fieldsForUnbound: List<IrField>
    ) = addFunctionOverride(invokeToOverride.owner) { function ->
        +irReturn(callInvokeSuspend(invokeSuspend, cloneLambda(function, constructor, fieldsForUnbound)))
    }

    private fun IrClass.addCreate(
        constructor: IrFunction,
        createToOverride: IrSimpleFunctionSymbol,
        fieldsForUnbound: List<IrField>
    ) = addFunctionOverride(createToOverride.owner) { function ->
        +irReturn(cloneLambda(function, constructor, fieldsForUnbound))
    }

    private fun IrBlockBodyBuilder.cloneLambda(
        scope: IrFunction,
        constructor: IrFunction,
        fieldsForUnbound: List<IrField>
    ): IrExpression {
        val constructorCall = irCall(constructor).also {
            for (typeParameter in constructor.parentAsClass.typeParameters) {
                it.putTypeArgument(typeParameter.index, typeParameter.defaultType)
            }
            it.putValueArgument(0, irGet(scope.valueParameters.last()))
        }
        if (fieldsForUnbound.isEmpty()) {
            return constructorCall
        }
        val result = irTemporary(constructorCall, "result")
        for ((index, field) in fieldsForUnbound.withIndex()) {
            +irSetField(irGet(result), field, irGet(scope.valueParameters[index]))
        }
        return irGet(result)
    }

    private fun IrBlockBodyBuilder.callInvokeSuspend(invokeSuspend: IrSimpleFunction, lambda: IrExpression): IrExpression =
        irCallOp(invokeSuspend.symbol, invokeSuspend.returnType, lambda, irUnit())

    // Primary constructor accepts parameters equal to function reference arguments + continuation and sets the fields.
    private fun IrClass.addPrimaryConstructorForLambda(superClass: IrClass, arity: Int): IrConstructor =
        addConstructor {
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            isPrimary = true
            returnType = defaultType
            visibility = DescriptorVisibilities.LOCAL
        }.also { constructor ->
            val completionParameterSymbol = constructor.addCompletionValueParameter()
            val superClassConstructor = superClass.constructors.single {
                it.valueParameters.size == 2 && it.valueParameters[0].type.isInt() && it.valueParameters[1].type.isNullableContinuation()
            }
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.putValueArgument(0, irInt(arity + 1))
                    it.putValueArgument(1, irGet(completionParameterSymbol))
                }
            }
        }
}

private class AddContinuationLowering(context: JvmBackendContext) : SuspendLoweringBase(context) {
    override fun lower(irFile: IrFile) {
        // This should be done after converting lambdas into classes to avoid breaking the invariant that
        // each lambda is referenced at most once while creating `$$forInline` methods.
        addContinuationObjectAndContinuationParameterToSuspendFunctions(irFile)
        addContinuationParameterToSuspendCalls(irFile)
    }

    private fun addContinuationParameterToSuspendCalls(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            val functionStack = mutableListOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                return super.visitFunction(declaration).also { functionStack.pop() }
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val transformed = super.visitFunctionReference(expression) as IrFunctionReference
                // The only references not yet transformed into objects are inline lambdas; the continuation
                // for those will be taken from the inline functions they are passed to, not the enclosing scope.
                return transformed.retargetToSuspendView(context, null) {
                    IrFunctionReferenceImpl(startOffset, endOffset, type, it, typeArgumentsCount, reflectionTarget, origin)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val transformed = super.visitCall(expression) as IrCall
                return transformed.retargetToSuspendView(context, functionStack.peek() ?: return transformed) {
                    IrCallImpl(startOffset, endOffset, type, it, origin, superQualifierSymbol)
                }
            }
        })
    }

    private fun generateContinuationClassForNamedFunction(
        irFunction: IrFunction,
        dispatchReceiverParameter: IrValueParameter?,
        attributeContainer: IrAttributeContainer,
        capturesCrossinline: Boolean
    ): IrClass =
        context.irFactory.buildClass {
            name = Name.special("<Continuation>")
            origin = JvmLoweredDeclarationOrigin.CONTINUATION_CLASS
            visibility = if (capturesCrossinline) DescriptorVisibilities.PUBLIC else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += context.ir.symbols.continuationImplClass.owner.defaultType
            parent = irFunction

            copyTypeParametersFrom(irFunction)
            val resultField = addField {
                origin = JvmLoweredDeclarationOrigin.CONTINUATION_CLASS_RESULT_FIELD
                name = Name.identifier(context.state.languageVersionSettings.dataFieldName())
                type = context.irBuiltIns.anyNType
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            }
            val capturedThisField = dispatchReceiverParameter?.let {
                addField {
                    name = Name.identifier("this$0")
                    type = it.type
                    origin = InnerClassesSupport.FIELD_FOR_OUTER_THIS
                    visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                    isFinal = true
                }
            }
            val labelField = addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaDescriptorVisibilities.PACKAGE_VISIBILITY)
            addConstructorForNamedFunction(capturedThisField, capturesCrossinline)
            addInvokeSuspendForNamedFunction(
                irFunction,
                resultField,
                labelField,
                capturedThisField,
                irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION
            )
            copyAttributes(attributeContainer)
        }

    private fun IrClass.addConstructorForNamedFunction(capturedThisField: IrField?, capturesCrossinline: Boolean): IrConstructor =
        addConstructor {
            isPrimary = true
            returnType = defaultType
            visibility = if (capturesCrossinline) DescriptorVisibilities.PUBLIC else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        }.also { constructor ->
            val capturedThisParameter = capturedThisField?.let { constructor.addValueParameter(it.name.asString(), it.type) }
            val completionParameterSymbol = constructor.addCompletionValueParameter()

            val superClassConstructor = context.ir.symbols.continuationImplClass.owner.constructors.single { it.valueParameters.size == 1 }
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                if (capturedThisField != null) {
                    +irSetField(irGet(thisReceiver!!), capturedThisField, irGet(capturedThisParameter!!))
                }
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.putValueArgument(0, irGet(completionParameterSymbol))
                }
            }
        }

    private fun IrClass.addInvokeSuspendForNamedFunction(
        irFunction: IrFunction,
        resultField: IrField,
        labelField: IrField,
        capturedThisField: IrField?,
        isStaticSuspendImpl: Boolean
    ) {
        val backendContext = context
        val invokeSuspend = context.ir.symbols.continuationImplClass.owner.functions
            .single { it.name == Name.identifier(INVOKE_SUSPEND_METHOD_NAME) }
        addFunctionOverride(invokeSuspend) { function ->
            +irSetField(irGet(function.dispatchReceiverParameter!!), resultField, irGet(function.valueParameters[0]))
            // There can be three kinds of suspend function call:
            // 1) direct call from another suspend function/lambda
            // 2) resume of coroutines via resumeWith call on continuation object
            // 3) recursive call
            // To distinguish case 1 from 2 and 3, we use simple INSTANCEOF check.
            // However, this check shows the same in cases 2 and 3.
            // Thus, we flip sign bit of label field in case 2 and leave it untouched in case 3.
            // Since this is literally case 2 (resumeWith calls invokeSuspend), flip the bit.
            val signBit = 1 shl 31
            +irSetField(
                irGet(function.dispatchReceiverParameter!!), labelField,
                irCallOp(
                    context.irBuiltIns.intClass.functions.single { it.owner.name == OperatorNameConventions.OR },
                    context.irBuiltIns.intType,
                    irGetField(irGet(function.dispatchReceiverParameter!!), labelField),
                    irInt(signBit)
                )
            )

            +irReturn(irCall(irFunction).also {
                for (i in irFunction.typeParameters.indices) {
                    it.putTypeArgument(i, typeParameters[i].defaultType)
                }
                val capturedThisValue = capturedThisField?.let { irField ->
                    irGetField(irGet(function.dispatchReceiverParameter!!), irField)
                }
                if (irFunction.dispatchReceiverParameter != null) {
                    it.dispatchReceiver = capturedThisValue
                }
                irFunction.extensionReceiverParameter?.let { extensionReceiverParameter ->
                    it.extensionReceiver = extensionReceiverParameter.type.defaultValue(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, backendContext
                    )
                }
                for ((i, parameter) in irFunction.valueParameters.dropLast(1).withIndex()) {
                    it.putValueArgument(i, parameter.type.defaultValue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, backendContext))
                }
                it.putValueArgument(
                    irFunction.valueParameters.size - 1,
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.dispatchReceiverParameter!!.symbol)
                )
                if (isStaticSuspendImpl) {
                    it.putValueArgument(0, capturedThisValue)
                }
            })
        }
    }

    private fun Name.toSuspendImplementationName() = when {
        isSpecial -> Name.special(asString() + SUSPEND_IMPL_NAME_SUFFIX)
        else -> Name.identifier(asString() + SUSPEND_IMPL_NAME_SUFFIX)
    }

    private fun createStaticSuspendImpl(irFunction: IrSimpleFunction): IrSimpleFunction {
        // Create static suspend impl method.
        val static = context.irFactory.createStaticFunctionWithReceivers(
            irFunction.parent,
            irFunction.name.toSuspendImplementationName(),
            irFunction,
            origin = JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION,
            isFakeOverride = false,
            copyMetadata = false
        )
        static.body = irFunction.moveBodyTo(static)
        static.copyAttributes(irFunction)
        // Rewrite the body of the original suspend method to forward to the new static method.
        irFunction.body = context.createIrBuilder(irFunction.symbol).irBlockBody {
            +irReturn(irCall(static).also {
                for (i in irFunction.typeParameters.indices) {
                    it.putTypeArgument(i, context.irBuiltIns.anyNType)
                }
                var i = 0
                if (irFunction.dispatchReceiverParameter != null) {
                    it.putValueArgument(i++, irGet(irFunction.dispatchReceiverParameter!!))
                }
                if (irFunction.extensionReceiverParameter != null) {
                    it.putValueArgument(i++, irGet(irFunction.extensionReceiverParameter!!))
                }
                for (parameter in irFunction.valueParameters) {
                    it.putValueArgument(i++, irGet(parameter))
                }
            })
        }
        return static
    }

    private fun addContinuationObjectAndContinuationParameterToSuspendFunctions(irFile: IrFile) {
        class MutableFlag(var capturesCrossinline: Boolean)
        irFile.accept(object : IrElementTransformer<MutableFlag?> {
            override fun visitClass(declaration: IrClass, data: MutableFlag?): IrStatement {
                declaration.transformDeclarationsFlat {
                    if (it is IrSimpleFunction && it.isSuspend)
                        return@transformDeclarationsFlat transformToView(it)
                    it.accept(this, null)
                    null
                }
                return declaration
            }

            private fun transformToView(function: IrSimpleFunction): List<IrFunction>? {
                val flag = MutableFlag(false)
                function.accept(this, flag)

                val view = function.suspendFunctionViewOrStub(context) as IrSimpleFunction
                val continuationParameter = view.continuationParameter()
                val parameterMap = function.explicitParameters.zip(view.explicitParameters.filter { it != continuationParameter }).toMap()
                view.body = function.moveBodyTo(view, parameterMap)

                val result = mutableListOf(view)
                if (function.body == null || !function.hasContinuation()) return result

                // This is a suspend function inside of SAM adapter.
                // The attribute of function reference is used for the SAM adapter.
                // So, we hack new attributes for continuation class.
                if (function.parentAsClass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
                    context.putLocalClassType(
                        function.attributeOwnerId,
                        Type.getObjectType("${context.getLocalClassType(function.parentAsClass)!!.internalName}$${function.name}$1")
                    )
                }

                if (flag.capturesCrossinline || function.isInline) {
                    result += context.irFactory.buildFun {
                        containerSource = view.containerSource
                        name = Name.identifier(view.name.asString() + FOR_INLINE_SUFFIX)
                        returnType = view.returnType
                        modality = view.modality
                        isSuspend = view.isSuspend
                        isInline = view.isInline
                        visibility = if (view.isInline) DescriptorVisibilities.PRIVATE else view.visibility
                        origin =
                            if (view.isInline) JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
                            else JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
                    }.apply {
                        annotations += view.annotations.map { it.deepCopyWithSymbols(this) }
                        copyParameterDeclarationsFrom(view)
                        copyAttributes(view)
                        generateErrorForInlineBody()
                    }
                }

                val newFunction = if (function.isOverridable) {
                    // Create static method for the suspend state machine method so that reentering the method
                    // does not lead to virtual dispatch to the wrong method.
                    createStaticSuspendImpl(view).also { result += it }
                } else view

                newFunction.body = context.createIrBuilder(newFunction.symbol).irBlockBody {
                    +generateContinuationClassForNamedFunction(
                        newFunction,
                        view.dispatchReceiverParameter,
                        function as IrAttributeContainer,
                        flag.capturesCrossinline
                    )
                    for (statement in newFunction.body!!.statements) {
                        +statement
                    }
                }
                return result
            }

            override fun visitFieldAccess(expression: IrFieldAccessExpression, data: MutableFlag?): IrExpression {
                if (expression.symbol.owner.origin == LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE)
                    data?.capturesCrossinline = true
                return super.visitFieldAccess(expression, data)
            }
        }, null)
    }
}

// Transform `suspend fun foo(params): RetType` into `fun foo(params, $completion: Continuation<RetType>): Any?`
// the result is called 'view', just to be consistent with old backend.
private fun IrFunction.suspendFunctionViewOrStub(context: JvmBackendContext): IrFunction {
    if (!isSuspend) return this
    return context.suspendFunctionOriginalToView.getOrPut(suspendFunctionOriginal()) { createSuspendFunctionStub(context) }
}

internal fun IrFunction.suspendFunctionOriginal(): IrFunction =
    if (this is IrSimpleFunction && isSuspend &&
        !isStaticInlineClassReplacement &&
        !isOrOverridesDefaultParameterStub() &&
        !isDefaultImplsFunction
    )
        attributeOwnerId as IrFunction
    else this

private fun IrSimpleFunction.isOrOverridesDefaultParameterStub(): Boolean =
    // Cannot use resolveFakeOverride here because of KT-36188.
    DFS.ifAny(
        listOf(this),
        { it.overriddenSymbols.map { it.owner } },
        { it.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER }
    )

val defaultImplsOrigins = setOf(
    IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_WITH_MOVED_RECEIVERS,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_WITH_MOVED_RECEIVERS_SYNTHETIC,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC,
    JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY_SYNTHETIC
)

private val IrSimpleFunction.isDefaultImplsFunction: Boolean
    get() = origin in defaultImplsOrigins

private fun IrFunction.createSuspendFunctionStub(context: JvmBackendContext): IrFunction {
    require(this.isSuspend && this is IrSimpleFunction)
    return factory.buildFun {
        updateFrom(this@createSuspendFunctionStub)
        name = this@createSuspendFunctionStub.name
        origin = this@createSuspendFunctionStub.origin
        returnType = context.irBuiltIns.anyNType
    }.also { function ->
        function.parent = parent

        function.annotations += annotations
        function.metadata = metadata

        function.copyAttributes(this)
        function.copyTypeParametersFrom(this)
        function.copyReceiverParametersFrom(this)

        if (origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE) {
            function.overriddenSymbols +=
                overriddenSymbols.map { it.owner.suspendFunctionViewOrStub(context).symbol as IrSimpleFunctionSymbol }
        }

        // The continuation parameter goes before the default argument mask(s) and handler for default argument stubs.
        // TODO: It would be nice if AddContinuationLowering could insert the continuation argument before default stub generation.
        val index = valueParameters.firstOrNull { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }?.index
            ?: valueParameters.size
        function.valueParameters += valueParameters.take(index).map { it.copyTo(function, index = it.index) }
        function.addValueParameter(
            SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType(context), JvmLoweredDeclarationOrigin.CONTINUATION_CLASS
        )
        function.valueParameters += valueParameters.drop(index).map { it.copyTo(function, index = it.index + 1) }
    }
}

private fun IrFunction.continuationType(context: JvmBackendContext): IrType {
    // For SuspendFunction{N}.invoke we need to generate INVOKEINTERFACE Function{N+1}.invoke(...Ljava/lang/Object;)...
    // instead of INVOKEINTERFACE Function{N+1}.invoke(...Lkotlin/coroutines/Continuation;)...
    val isInvokeOfNumberedSuspendFunction = (parent as? IrClass)?.defaultType?.isSuspendFunction() == true
    val isInvokeOfNumberedFunction = (parent as? IrClass)?.fqNameWhenAvailable?.asString()?.let {
        it.startsWith("kotlin.jvm.functions.Function") && it.removePrefix("kotlin.jvm.functions.Function").all { c -> c.isDigit() }
    } == true
    return if (isInvokeOfNumberedSuspendFunction || isInvokeOfNumberedFunction)
        context.irBuiltIns.anyNType
    else
        context.ir.symbols.continuationClass.typeWith(returnType)
}

private fun <T : IrMemberAccessExpression<IrFunctionSymbol>> T.retargetToSuspendView(
    context: JvmBackendContext,
    caller: IrFunction?,
    copyWithTargetSymbol: T.(IrSimpleFunctionSymbol) -> T
): T {
    // Calls inside continuation are already generated with continuation parameter as well as calls to suspendImpls
    if (!symbol.owner.isSuspend || caller?.isInvokeSuspendOfContinuation() == true
        || symbol.owner.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION
        || symbol.owner.continuationParameter() != null
    ) return this
    val view = symbol.owner.suspendFunctionViewOrStub(context)
    if (view == symbol.owner) return this

    // While the new callee technically returns `<original type> | COROUTINE_SUSPENDED`, the latter case is handled
    // by a method visitor so at an IR overview we don't need to consider it.
    return copyWithTargetSymbol(view.symbol as IrSimpleFunctionSymbol).also {
        it.copyAttributes(this)
        it.copyTypeArgumentsFrom(this)
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        val continuationIndex = view.continuationParameter()!!.index
        for (i in 0 until valueArgumentsCount) {
            it.putValueArgument(i + if (i >= continuationIndex) 1 else 0, getValueArgument(i))
        }
        if (caller != null) {
            // At this point the only LOCAL_FUNCTION_FOR_LAMBDAs are inline and crossinline lambdas.
            val continuation = if (caller.originalFunction.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA)
                context.fakeContinuation
            else
                IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.continuationParameter()?.symbol
                        ?: throw AssertionError("${caller.render()} has no continuation; can't call ${symbol.owner.render()}")
                )
            it.putValueArgument(continuationIndex, continuation)
        }
    }
}

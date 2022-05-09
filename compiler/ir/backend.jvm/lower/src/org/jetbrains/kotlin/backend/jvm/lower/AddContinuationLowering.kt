/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Type

internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation classes and parameters to suspend functions",
    prerequisite = setOf(suspendLambdaPhase, localDeclarationsPhase, tailCallOptimizationPhase)
)

private class AddContinuationLowering(context: JvmBackendContext) : SuspendLoweringUtils(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
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
                    IrFunctionReferenceImpl.fromSymbolOwner(startOffset, endOffset, type, it, typeArgumentsCount, reflectionTarget, origin)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val transformed = super.visitCall(expression) as IrCall
                return transformed.retargetToSuspendView(context, functionStack.peek() ?: return transformed) {
                    IrCallImpl.fromSymbolOwner(
                        startOffset, endOffset, type, it,
                        origin = origin, superQualifierSymbol = superQualifierSymbol
                    )
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
                name = Name.identifier(CONTINUATION_RESULT_FIELD_NAME)
                type = context.ir.symbols.resultOfAnyType
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            }
            val capturedThisField = dispatchReceiverParameter?.let {
                addField {
                    name = Name.identifier("this$0")
                    type = it.type
                    origin = IrDeclarationOrigin.FIELD_FOR_OUTER_THIS
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
        addFunctionOverride(invokeSuspend, irFunction.startOffset, irFunction.endOffset) { function ->
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

    private fun Name.toSuspendImplementationName(): Name =
        Name.guessByFirstCharacter(asString() + SUSPEND_IMPL_NAME_SUFFIX)

    private fun createStaticSuspendImpl(irFunction: IrSimpleFunction): IrSimpleFunction {
        // Create static suspend impl method.
        val static = context.irFactory.createStaticFunctionWithReceivers(
            irFunction.parent,
            irFunction.name.toSuspendImplementationName(),
            irFunction,
            origin = JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION,
            modality = Modality.OPEN,
            visibility = if (irFunction.parentAsClass.isJvmInterface)
                DescriptorVisibilities.PUBLIC
            else
                JavaDescriptorVisibilities.PACKAGE_VISIBILITY,
            isFakeOverride = false,
            copyMetadata = false
        )
        static.body = irFunction.moveBodyTo(static)
        // Fixup dispatch parameter to outer class
        if (irFunction.parentAsClass.isInner) {
            val movedDispatchParameter = static.valueParameters[0]
            assert(movedDispatchParameter.origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER) {
                "MOVED_DISPATCH_RECEIVER should be the first parameter in ${static.render()}"
            }
            static.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    val owner = expression.symbol.owner
                    if (owner is IrValueParameter && isInstanceReceiverOfOuterClass(owner)) {
                        // If inner class has inner classes, we need to traverse this$0 chain to get to captured dispatch receivers
                        var cursor = irFunction.parentAsClass
                        var value: IrExpression = IrGetValueImpl(expression.startOffset, expression.endOffset, movedDispatchParameter.symbol)
                        while (cursor != owner.parent) {
                            val outerThisField = context.innerClassesSupport.getOuterThisField(cursor)
                            value = IrGetFieldImpl(
                                expression.startOffset, expression.endOffset, outerThisField.symbol, outerThisField.type, value
                            )
                            cursor = cursor.parentAsClass
                        }
                        return value
                    }
                    return super.visitGetValue(expression)
                }

                private fun isInstanceReceiverOfOuterClass(param: IrValueParameter): Boolean {
                    if (param.origin != IrDeclarationOrigin.INSTANCE_RECEIVER) return false
                    if (param.parent !is IrClass) return false

                    var cursor = irFunction.parentAsClass.parent
                    while (cursor is IrClass) {
                        if (cursor == param.parent) return true
                        cursor = (cursor as IrClass).parent
                    }
                    return false
                }
            })
        }
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

            private fun transformToView(function: IrSimpleFunction): List<IrFunction> {
                val flag = MutableFlag(false)
                function.accept(this, flag)

                val view = function.suspendFunctionViewOrStub(context)
                val continuationParameter = view.continuationParameter()
                val parameterMap = function.explicitParameters.zip(view.explicitParameters.filter { it != continuationParameter }).toMap()
                view.body = function.moveBodyTo(view, parameterMap)

                val result = mutableListOf(view)
                if (function.body == null || !function.hasContinuation()) return result

                // Sometimes, suspend methods of SAM adapters or function references require a continuation class.
                // However, the attribute owner is used to store the name of the SAM adapter or the function reference itself.
                // So here we add a local class name using the suspend method itself as the key.
                if (function.parentAsClass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
                    function.parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                ) {
                    context.putLocalClassType(
                        function.attributeOwnerId,
                        Type.getObjectType("${context.getLocalClassType(function.parentAsClass)!!.internalName}$${function.name}$1")
                    )
                }

                if (flag.capturesCrossinline || function.isInline) {
                    result += context.irFactory.buildFun {
                        containerSource = view.containerSource
                        name = Name.identifier(context.methodSignatureMapper.mapFunctionName(view) + FOR_INLINE_SUFFIX)
                        returnType = view.returnType
                        modality = view.modality
                        isSuspend = view.isSuspend
                        isInline = view.isInline
                        visibility = if (view.isInline) DescriptorVisibilities.PRIVATE else view.visibility
                        origin =
                            if (view.isInline) JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
                            else JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
                    }.apply {
                        copyAnnotationsFrom(view)
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
                    if (newFunction.body is IrExpressionBody) {
                        +irReturn(newFunction.body!!.statements[0] as IrExpression)
                    } else {
                        for (statement in newFunction.body!!.statements) {
                            +statement
                        }
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
private fun IrSimpleFunction.suspendFunctionViewOrStub(context: JvmBackendContext): IrSimpleFunction {
    if (!isSuspend) return this
    // If superinterface is in another file, the bridge to default method will already have continuation parameter,
    // so skip it. See KT-47549.
    if (origin == JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE &&
        valueParameters.lastOrNull()?.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS
    ) return this
    // We need to use suspend function originals here, since if we use 'this' here,
    // turing FlowCollector into 'fun interface' leads to AbstractMethodError. See KT-49294.
    return context.suspendFunctionOriginalToView.getOrPut(suspendFunctionOriginal()) { createSuspendFunctionStub(context) }
}

private fun IrSimpleFunction.createSuspendFunctionStub(context: JvmBackendContext): IrSimpleFunction {
    require(this.isSuspend)
    return factory.buildFun {
        updateFrom(this@createSuspendFunctionStub)
        name = this@createSuspendFunctionStub.name
        origin = this@createSuspendFunctionStub.origin
        returnType = context.irBuiltIns.anyNType
    }.also { function ->
        function.parent = parent

        function.annotations += annotations
        function.metadata = metadata
        function.contextReceiverParametersCount = contextReceiverParametersCount

        function.copyAttributes(this)
        function.copyTypeParametersFrom(this)
        val substitutionMap = makeTypeParameterSubstitutionMap(this, function)
        function.copyReceiverParametersFrom(this, substitutionMap)

        function.overriddenSymbols += overriddenSymbols.map { it.owner.suspendFunctionViewOrStub(context).symbol }

        // The continuation parameter goes before the default argument mask(s) and handler for default argument stubs.
        // TODO: It would be nice if AddContinuationLowering could insert the continuation argument before default stub generation.
        val index = valueParameters.firstOrNull { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }?.index
            ?: valueParameters.size
        function.valueParameters += valueParameters.take(index).map {
            it.copyTo(function, index = it.index, type = it.type.substitute(substitutionMap))
        }
        function.addValueParameter(
            SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
            continuationType(context).substitute(substitutionMap),
            JvmLoweredDeclarationOrigin.CONTINUATION_CLASS
        )
        function.valueParameters += valueParameters.drop(index).map {
            it.copyTo(function, index = it.index + 1, type = it.type.substitute(substitutionMap))
        }
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
    val owner = symbol.owner
    if (owner !is IrSimpleFunction || !owner.isSuspend || caller?.isInvokeSuspendOfContinuation() == true
        || owner.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION
        || owner.continuationParameter() != null
    ) return this
    val view = owner.suspendFunctionViewOrStub(context)
    if (view == owner) return this

    // While the new callee technically returns `<original type> | COROUTINE_SUSPENDED`, the latter case is handled
    // by a method visitor so at an IR overview we don't need to consider it.
    return copyWithTargetSymbol(view.symbol).also {
        it.copyAttributes(this)
        it.copyTypeArgumentsFrom(this)
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        val continuationParameter = view.continuationParameter()!!
        for (i in 0 until valueArgumentsCount) {
            it.putValueArgument(i + if (i >= continuationParameter.index) 1 else 0, getValueArgument(i))
        }
        if (caller != null) {
            val continuation = if (caller.origin == JvmLoweredDeclarationOrigin.INLINE_LAMBDA)
                IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, continuationParameter.type, JvmLoweredStatementOrigin.FAKE_CONTINUATION)
            else
                IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.continuationParameter()?.symbol
                        ?: throw AssertionError("${caller.render()} has no continuation; can't call ${owner.render()}")
                )
            it.putValueArgument(continuationParameter.index, continuation)
        }
    }
}

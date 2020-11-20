/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isReadOfCrossinline
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.codegen.coroutines.COROUTINE_LABEL_FIELD_NAME
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val suspendLambdaPhase = makeIrFilePhase(
    ::SuspendLambdaLowering,
    "SuspendLambda",
    "Transform suspend lambdas into continuation classes"
)

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

internal abstract class SuspendLoweringUtils(protected val context: JvmBackendContext) {
    protected fun IrClass.addFunctionOverride(
        function: IrSimpleFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrSimpleFunction {
        val overriddenType = superTypes.single { it.classifierOrFail == function.parentAsClass.symbol }
        val typeSubstitution = (overriddenType.classifierOrFail.owner as IrClass).typeParameters
            .map { it.symbol }
            .zip((overriddenType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }) // No star projections in this lowering
            .toMap()
        return addFunction(
            function.name.asString(), function.returnType.substitute(typeSubstitution),
            startOffset = startOffset, endOffset = endOffset
        ).apply {
            overriddenSymbols = listOf(function.symbol)
            valueParameters = function.valueParameters.map { it.copyTo(this, type = it.type.substitute(typeSubstitution)) }
        }
    }

    protected fun IrClass.addFunctionOverride(
        function: IrSimpleFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        makeBody: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrSimpleFunction =
        addFunctionOverride(function, startOffset, endOffset).apply {
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

private class SuspendLambdaLowering(context: JvmBackendContext) : SuspendLoweringUtils(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val inlineReferences = IrInlineReferenceLocator.scan(context, irFile)
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitBlock(expression: IrBlock): IrExpression {
                val reference = expression.statements.lastOrNull() as? IrFunctionReference ?: return super.visitBlock(expression)
                if (reference.isSuspend && reference.origin.isLambda && reference !in inlineReferences) {
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
            assert(reference.getArgumentsWithIr().isEmpty()) { "lambda with bound arguments: ${reference.render()}" }
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
            val functionNType = functionNClass.typeWith(
                function.explicitParameters.subList(0, arity).map { it.type }
                        + function.continuationType()
                        + context.irBuiltIns.anyNType
            )
            superTypes = listOf(suspendLambda.defaultType, functionNType)

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
            val invokeSuspend = addInvokeSuspendForLambda(function, suspendLambda, parametersFields)
            if (function.capturesCrossinline()) {
                addInvokeSuspendForInlineForLambda(invokeSuspend)
            }
            if (createToOverride != null) {
                addInvokeCallingCreate(addCreate(constructor, createToOverride, parametersFields), invokeSuspend, invokeToOverride)
            } else {
                addInvokeCallingConstructor(constructor, invokeSuspend, invokeToOverride, parametersFields)
            }

            this.metadata = function.metadata
            context.suspendLambdaToOriginalFunctionMap[attributeOwnerId as IrFunctionReference] = function
        }

    private fun IrClass.addInvokeSuspendForLambda(irFunction: IrFunction, suspendLambda: IrClass, fields: List<IrField>): IrSimpleFunction {
        val superMethod = suspendLambda.functions.single {
            it.name.asString() == INVOKE_SUSPEND_METHOD_NAME && it.valueParameters.size == 1 &&
                    it.valueParameters[0].type.isKotlinResult()
        }
        return addFunctionOverride(superMethod, irFunction.startOffset, irFunction.endOffset).apply {
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
            origin = JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
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

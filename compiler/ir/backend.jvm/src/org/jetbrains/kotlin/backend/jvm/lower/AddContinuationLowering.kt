/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.defaultValue
import org.jetbrains.kotlin.backend.jvm.codegen.isInvokeSuspendForInlineOfLambda
import org.jetbrains.kotlin.backend.jvm.codegen.isInvokeSuspendOfContinuation
import org.jetbrains.kotlin.backend.jvm.codegen.isInvokeSuspendOfLambda
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation classes and parameters to suspend functions and transform suspend lambdas into continuations",
    prerequisite = setOf(localDeclarationsPhase, tailCallOptimizationPhase)
)

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val suspendLambdas = findSuspendAndInlineLambdas(irFile)
        addContinuationObjectAndContinuationParameterToSuspendFunctions(irFile)
        transformSuspendLambdasIntoContinuations(irFile, suspendLambdas)
        addContinuationParameterToSuspendCallsAndUpdateNonLocalReturns(irFile)
    }

    private fun addContinuationParameterToSuspendCallsAndUpdateNonLocalReturns(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            val functionStack = mutableListOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                return super.visitFunction(declaration).also { functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                // This is a property, no need to add continuation parameter, since this cannot be suspend call
                if (functionStack.isEmpty()) return super.visitCall(expression)
                val caller = functionStack.peek()!!
                return (super.visitCall(expression) as IrCall)
                    .createSuspendFunctionCallViewIfNeeded(context, caller)
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                val ret = super.visitReturn(expression) as IrReturn
                val irFunction = expression.returnTargetSymbol.owner as? IrFunction ?: return ret
                val targetViewOrStub = irFunction.suspendFunctionViewOrStub(context)
                return IrReturnImpl(ret.startOffset, ret.endOffset, ret.type, targetViewOrStub.symbol, ret.value)
            }
        })
    }

    private fun transformSuspendLambdasIntoContinuations(irFile: IrFile, suspendLambdas: List<SuspendLambdaInfo>) {
        for (lambda in suspendLambdas) {
            (lambda.function.parent as IrDeclarationContainer).declarations.remove(lambda.function)
        }
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                if (!expression.isSuspend)
                    return expression
                val info = suspendLambdas.singleOrNull { it.function == expression.symbol.owner } ?: return expression
                return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).run {
                    val expressionArguments = expression.getArguments().map { it.second }
                    irBlock {
                        +generateContinuationClassForLambda(
                            info,
                            currentDeclarationParent,
                            (currentFunction?.irElement as? IrFunction)?.isInline == true
                        )
                        val constructor = info.constructor
                        assert(constructor.valueParameters.size == expressionArguments.size + 1) {
                            "Inconsistency between callable reference to suspend lambda and the corresponding continuation"
                        }
                        +irCall(constructor.symbol).apply {
                            for (typeParameter in info.constructor.parentAsClass.typeParameters) {
                                putTypeArgument(typeParameter.index, expression.getTypeArgument(typeParameter.index))
                            }
                            expressionArguments.forEachIndexed { index, argument ->
                                putValueArgument(index, argument)
                            }
                            // Pass null as completion parameter
                            putValueArgument(expressionArguments.size, irNull())
                        }
                    }
                }.also { it.transformChildrenVoid(this) }
            }
        })
    }

    private fun generateContinuationClassForLambda(
        info: SuspendLambdaInfo,
        parent: IrDeclarationParent,
        insideInlineFunction: Boolean
    ): IrClass {
        val suspendLambda = context.ir.symbols.suspendLambdaClass.owner
        return suspendLambda.createContinuationClassFor(
            parent,
            JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA,
            // Since inline functions can be inlined to different package, we should generate lambdas inside these functions
            // as public
            if (insideInlineFunction) Visibilities.PUBLIC else JavaVisibilities.PACKAGE_VISIBILITY
        ).apply {
            copyAttributes(info.reference)
            copyTypeParametersFrom(info.function)
            val functionNClass = context.ir.symbols.getJvmFunctionClass(info.arity + 1)
            superTypes.add(
                IrSimpleTypeImpl(
                    functionNClass,
                    hasQuestionMark = false,
                    arguments = (info.function.explicitParameters.subList(0, info.arity).map { it.type }
                            + info.function.continuationType() + info.function.returnType)
                        .map { makeTypeProjection(it, Variance.INVARIANT) },
                    annotations = emptyList()
                )
            )

            addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType)

            val receiverField = info.function.extensionReceiverParameter?.let {
                assert(info.arity != 0)
                // Do not put '$' at the start, to avoid being caught by inlineCodegenUtils.isCapturedFieldName()
                addField("p\$", it.type)
            }

            val parametersFields = info.function.valueParameters.map { addField(it.name, it.type) }
            val parametersWithoutArguments = parametersFields.withIndex()
                .mapNotNull { (i, field) -> if (info.reference.getValueArgument(i) == null) field else null }
            val parametersWithArguments = parametersFields.withIndex()
                .filter { info.reference.getValueArgument(it.index) != null }
            val fieldsForArguments = parametersWithArguments.map(IndexedValue<IrField>::value)
            val constructor = addPrimaryConstructorForLambda(info.arity, info.reference, fieldsForArguments)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.valueParameters.size == info.arity + 1 && it.owner.name.asString() == "invoke"
            }
            val invokeSuspend = addInvokeSuspendForLambda(info.function, parametersFields, receiverField)
            if (info.capturesCrossinline) {
                addInvokeSuspendForInlineForLambda(invokeSuspend, info.function, parametersFields, receiverField)
            }
            info.function.parentAsClass.declarations.remove(info.function)
            if (info.arity <= 1) {
                val singleParameterField = receiverField ?: parametersWithoutArguments.singleOrNull()
                val create = addCreate(constructor, suspendLambda, info, parametersWithArguments, singleParameterField)
                addInvokeCallingCreate(create, invokeSuspend, invokeToOverride, singleParameterField)
            } else {
                addInvokeCallingConstructor(
                    constructor,
                    invokeSuspend,
                    invokeToOverride,
                    fieldsForArguments,
                    listOfNotNull(receiverField) + parametersWithoutArguments
                )
            }

            context.suspendLambdaToOriginalFunctionMap[attributeOwnerId as IrFunctionReference] = info.function

            info.constructor = constructor
        }
    }

    private fun IrClass.addInvokeSuspendForLambda(
        irFunction: IrFunction,
        fields: List<IrField>,
        receiverField: IrField?
    ): IrFunction {
        val superMethod = context.ir.symbols.suspendLambdaClass.functions.single {
            it.owner.name.asString() == INVOKE_SUSPEND_METHOD_NAME && it.owner.valueParameters.size == 1 &&
                    it.owner.valueParameters[0].type.isKotlinResult()
        }.owner
        return addFunctionOverride(superMethod).also { it.copySuspendLambdaBodyFrom(irFunction, receiverField, fields) }
    }

    private fun IrClass.addInvokeSuspendForInlineForLambda(
        invokeSuspend: IrFunction,
        irFunction: IrFunction,
        fields: List<IrField>,
        receiverField: IrField?
    ): IrFunction {
        return addFunction(
            INVOKE_SUSPEND_METHOD_NAME + FOR_INLINE_SUFFIX,
            context.irBuiltIns.anyNType,
            Modality.FINAL,
            origin = JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
        ).apply {
            invokeSuspend.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
        }.also { it.copySuspendLambdaBodyFrom(irFunction, receiverField, fields) }
    }

    private fun IrSimpleFunction.copySuspendLambdaBodyFrom(
        irFunction: IrFunction,
        receiverField: IrField?,
        fields: List<IrField>
    ) {
        body = irFunction.body?.deepCopyWithSymbols(this)
        body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (expression.symbol.owner == irFunction.extensionReceiverParameter) {
                    assert(receiverField != null)
                    return IrGetFieldImpl(
                        expression.startOffset,
                        expression.endOffset,
                        receiverField!!.symbol,
                        receiverField.type
                    ).also {
                        it.receiver =
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter!!.symbol)
                    }
                } else if (expression.symbol.owner == irFunction.dispatchReceiverParameter) {
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, dispatchReceiverParameter!!.symbol)
                }
                val field = fields.find { it.name == expression.symbol.owner.name } ?: return expression
                return IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, field.type).also {
                    it.receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter!!.symbol)
                }
            }

            // If the suspend lambda body contains declarations of other classes (for other lambdas),
            // do not rewrite those. In particular, that could lead to rewriting of returns in nested
            // lambdas to unintended non-local returns.
            override fun visitClass(declaration: IrClass): IrStatement {
                return declaration
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                val ret = super.visitReturn(expression) as IrReturn
                return IrReturnImpl(ret.startOffset, ret.endOffset, ret.type, symbol, ret.value)
            }
        })
    }

    private fun IrDeclarationContainer.addFunctionOverride(
        function: IrSimpleFunction,
        modality: Modality = Modality.FINAL
    ): IrSimpleFunction =
        addFunction(function.name.asString(), function.returnType, modality).apply {
            overriddenSymbols.add(function.symbol)
            function.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
        }

    // Invoke function in lambdas is responsible for
    //   1) calling `create`
    //   2) starting newly created coroutine by calling `invokeSuspend`.
    // Thus, it creates a clone of suspend lambda and starts it.
    private fun IrClass.addInvokeCallingCreate(
        create: IrFunction,
        invokeSuspend: IrFunction,
        invokeToOverride: IrSimpleFunctionSymbol,
        receiverField: IrField?
    ) {
        val unitClass = context.irBuiltIns.unitClass
        val unitField = context.declarationFactory.getFieldForObjectInstance(unitClass.owner)
        addFunctionOverride(invokeToOverride.owner).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                // Call `create`
                val newlyCreatedObject = irTemporary(irCall(create).also { createCall ->
                    createCall.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                    if (receiverField != null) {
                        createCall.putValueArgument(0, irGet(function.valueParameters[0]))
                    }
                    createCall.putValueArgument(if (receiverField != null) 1 else 0, irGet(function.valueParameters.last()))
                }, "create")
                // Start coroutine
                +irReturn(irCall(invokeSuspend).also { invokeSuspendCall ->
                    invokeSuspendCall.dispatchReceiver = irGet(newlyCreatedObject)
                    invokeSuspendCall.putValueArgument(0, irGetField(null, unitField))
                })
            }
        }
    }

    // The same as @see addInvokeCallingCreate, but without `create`.
    // In old BE 'create' function was responsible for putting arguments into fields, but in IR_BE we do not generate create,
    // unless suspend lambda has no or one parameter, including extension receiver.
    // This is because 'create' is called only from 'createCoroutineUnintercepted' from stdlib. And we have only versions
    // for suspend lambdas without parameters and for ones with exactly one parameter (or extension receiver).
    // And since we still value method count, we do not want to inflate it with unnecessary functions.
    // Thus, we put arguments into fields in 'invoke', instead of `create`.
    private fun IrClass.addInvokeCallingConstructor(
        constructor: IrFunction,
        invokeSuspend: IrFunction,
        invokeToOverride: IrSimpleFunctionSymbol,
        parametersWithArguments: List<IrField>,
        parametersWithoutArguments: List<IrField>
    ) {
        val unitClass = context.irBuiltIns.unitClass
        val unitField = context.declarationFactory.getFieldForObjectInstance(unitClass.owner)
        addFunctionOverride(invokeToOverride.owner).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                // Create a copy
                val newlyCreatedObject = irTemporary(irCall(constructor).also { constructorCall ->
                    for (typeParameter in typeParameters) {
                        constructorCall.putTypeArgument(typeParameter.index, typeParameter.defaultType)
                    }
                    for ((index, field) in parametersWithArguments.withIndex()) {
                        constructorCall.putValueArgument(index, irGetField(irGet(function.dispatchReceiverParameter!!), field))
                    }
                    constructorCall.putValueArgument(parametersWithArguments.size, irGet(function.valueParameters.last()))
                }, "constructor")
                // Move parameters into fields (instead of `create`)
                if (parametersWithoutArguments.isNotEmpty()) {
                    for ((index, param) in function.valueParameters.dropLast(1).withIndex()) {
                        +irSetField(irGet(newlyCreatedObject), parametersWithoutArguments[index], irGet(param))
                    }
                }
                // Start coroutine
                +irReturn(irCall(invokeSuspend).also { invokeSuspendCall ->
                    invokeSuspendCall.dispatchReceiver = irGet(newlyCreatedObject)
                    invokeSuspendCall.putValueArgument(0, irGetField(null, unitField))
                })
            }
        }
    }

    private fun IrClass.addCreate(
        constructor: IrFunction,
        superType: IrClass,
        info: SuspendLambdaInfo,
        parametersWithArguments: List<IndexedValue<IrField>>,
        singleParameterField: IrField?
    ): IrFunction {
        val create = superType.functions.single {
            it.name.asString() == "create" && it.valueParameters.size == info.arity + 1 &&
                    it.valueParameters.last().type.isContinuation() &&
                    if (info.arity == 1) it.valueParameters.first().type.isNullableAny() else true
        }
        return addFunctionOverride(create).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                var index = 0
                val constructorCall = irCall(constructor).also {
                    for (typeParameter in typeParameters) {
                        it.putTypeArgument(typeParameter.index, typeParameter.defaultType)
                    }
                    for ((i, field) in parametersWithArguments) {
                        if (info.reference.getValueArgument(i) == null) continue
                        it.putValueArgument(index++, irGetField(irGet(function.dispatchReceiverParameter!!), field))
                    }
                    it.putValueArgument(index, irGet(function.valueParameters.last()))
                }
                if (singleParameterField != null) {
                    assert(function.valueParameters.size == 2)
                    val result = irTemporary(constructorCall, "result")
                    +irSetField(irGet(result), singleParameterField, irGet(function.valueParameters.first()))
                    +irReturn(irGet(result))
                } else {
                    assert(function.valueParameters.size == 1)
                    +irReturn(constructorCall)
                }
            }
        }
    }

    private fun IrClass.createContinuationClassFor(
        parent: IrDeclarationParent,
        newOrigin: IrDeclarationOrigin,
        newVisibility: Visibility
    ): IrClass = buildClass {
        name = Name.special("<Continuation>")
        origin = newOrigin
        visibility = newVisibility
    }.also { irClass ->
        irClass.createImplicitParameterDeclarationWithWrappedDescriptor()
        irClass.superTypes.add(defaultType)

        irClass.parent = parent
    }

    // Primary constructor accepts parameters equal to function reference arguments + continuation and sets the fields.
    private fun IrClass.addPrimaryConstructorForLambda(
        arity: Int,
        reference: IrFunctionReference,
        fields: List<IrField>
    ): IrConstructor =
        addConstructor {
            isPrimary = true
            returnType = defaultType
        }.also { constructor ->
            for ((param, arg) in reference.getArguments()) {
                constructor.addValueParameter(name = param.name.asString(), type = arg.type)
            }
            val completionParameterSymbol = constructor.addCompletionValueParameter()

            val superClassConstructor = context.ir.symbols.suspendLambdaClass.owner.constructors.single {
                it.valueParameters.size == 2 && it.valueParameters[0].type.isInt() && it.valueParameters[1].type.isNullableContinuation()
            }
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.putValueArgument(0, irInt(arity + 1))
                    it.putValueArgument(1, irGet(completionParameterSymbol))
                }

                for ((index, param) in constructor.valueParameters.dropLast(1).withIndex()) {
                    +irSetField(irGet(thisReceiver!!), fields[index], irGet(param))
                }
            }
        }

    private fun IrFunction.addCompletionValueParameter(): IrValueParameter =
        addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType())

    private fun IrFunction.continuationType(): IrType =
        context.ir.symbols.continuationClass.typeWith(returnType).makeNullable()

    private fun generateContinuationClassForNamedFunction(
        irFunction: IrFunction,
        dispatchReceiverParameter: IrValueParameter?,
        attributeContainer: IrAttributeContainer
    ): IrClass {
        return context.ir.symbols.continuationImplClass.owner
            .createContinuationClassFor(irFunction, JvmLoweredDeclarationOrigin.CONTINUATION_CLASS, JavaVisibilities.PACKAGE_VISIBILITY)
            .apply {
                copyTypeParametersFrom(irFunction)
                val resultField = addField(
                    context.state.languageVersionSettings.dataFieldName(),
                    context.irBuiltIns.anyType,
                    JavaVisibilities.PACKAGE_VISIBILITY
                )
                val capturedThisField = dispatchReceiverParameter?.let { addField("this\$0", it.type) }
                val labelField = addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaVisibilities.PACKAGE_VISIBILITY)
                addConstructorForNamedFunction(capturedThisField)
                addInvokeSuspendForNamedFunction(
                    irFunction,
                    resultField,
                    labelField,
                    capturedThisField,
                    irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION
                )
                copyAttributes(attributeContainer)
            }
    }

    private fun IrClass.addConstructorForNamedFunction(capturedThisField: IrField?): IrConstructor = addConstructor {
        isPrimary = true
        returnType = defaultType
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


    private fun Name.toSuspendImplementationName() = when {
        isSpecial -> Name.special(asString() + SUSPEND_IMPL_NAME_SUFFIX)
        else -> Name.identifier(asString() + SUSPEND_IMPL_NAME_SUFFIX)
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
        addFunctionOverride(invokeSuspend).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
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
    }

    private fun createStaticSuspendImpl(irFunction: IrSimpleFunction): IrSimpleFunction {
        // Create static suspend impl method.
        val static = createStaticFunctionWithReceivers(
            irFunction.parent,
            irFunction.name.toSuspendImplementationName(),
            irFunction,
            origin = JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION,
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
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionsStack = arrayListOf<IrFunction>()
            private val suspendFunctionsCapturingCrossinline = mutableSetOf<IrFunction>()
            private val functionsToAdd = arrayListOf<MutableSet<IrFunction>>()

            override fun visitClass(declaration: IrClass): IrStatement {
                functionsToAdd.push(mutableSetOf())
                return (super.visitClass(declaration) as IrClass).also { irClass ->
                    for (function in functionsToAdd.pop()) {
                        function.parent = irClass
                        irClass.declarations.add(function)
                    }
                }
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionsStack.push(declaration)
                val function = super.visitFunction(declaration) as IrFunction
                functionsStack.pop()
                if (skip(function)) return function

                val view = function.getOrCreateSuspendFunctionViewIfNeeded(context) as IrSimpleFunction

                if (withoutContinuationClass(function)) return view

                if (function in suspendFunctionsCapturingCrossinline || function.isInline) {
                    val newFunction = buildFunWithDescriptorForInlining(view.descriptor) {
                        name = Name.identifier(view.name.asString() + FOR_INLINE_SUFFIX)
                        returnType = view.returnType
                        modality = view.modality
                        isSuspend = view.isSuspend
                        isInline = view.isInline
                        origin =
                            if (view.isInline) JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
                            else JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
                    }.apply {
                        copyTypeParameters(view.typeParameters)
                        dispatchReceiverParameter = view.dispatchReceiverParameter?.copyTo(this)
                        extensionReceiverParameter = view.extensionReceiverParameter?.copyTo(this)
                        view.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
                        body = view.copyBodyTo(this)
                        copyAttributes(view)
                    }
                    registerNewFunction(newFunction)
                }

                val newFunction = if ((function as IrSimpleFunction).isOverridable) {
                    // Create static method for the suspend state machine method so that reentering the method
                    // does not lead to virtual dispatch to the wrong method.
                    registerNewFunction(view)
                    createStaticSuspendImpl(view)
                } else view

                newFunction.body = context.createIrBuilder(newFunction.symbol).irBlockBody {
                    +generateContinuationClassForNamedFunction(
                        newFunction,
                        view.dispatchReceiverParameter,
                        declaration as IrAttributeContainer
                    )
                    for (statement in newFunction.body!!.statements) {
                        +statement
                    }
                }
                return newFunction
            }

            // TODO: Merge with `knownToBeTailCall`
            private fun withoutContinuationClass(function: IrFunction): Boolean =
                function.body == null ||
                        function.parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
                        function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                        function.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE

            private fun skip(function: IrFunction) =
                !function.isSuspend ||
                        function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA

            private fun registerNewFunction(function: IrFunction) {
                functionsToAdd.peek()!!.add(function)
            }

            override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
                val result = super.visitFieldAccess(expression)
                val function = functionsStack.peek() ?: return result
                if (function.isSuspend &&
                    expression.symbol.owner.origin == LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
                ) {
                    suspendFunctionsCapturingCrossinline += function
                }
                return result
            }
        })
    }

    private fun findSuspendAndInlineLambdas(irElement: IrElement): List<SuspendLambdaInfo> {
        val suspendLambdas = arrayListOf<SuspendLambdaInfo>()
        val capturesCrossinline = mutableSetOf<IrCallableReference>()
        val inlineReferences = mutableSetOf<IrCallableReference>()
        irElement.acceptChildrenVoid(object : IrInlineReferenceLocator(context) {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitInlineReference(argument: IrCallableReference) {
                inlineReferences.add(argument)
                val getValue = (argument as? IrGetValue) ?: return
                val parameter = getValue.symbol.owner as? IrValueParameter ?: return
                if (parameter.isCrossinline) {
                    capturesCrossinline += argument
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                if (expression.isSuspend && expression !in inlineReferences && expression.origin == IrStatementOrigin.LAMBDA) {
                    var expressionCapturesCrossinline = false
                    for (i in 0 until expression.valueArgumentsCount) {
                        val getValue = expression.getValueArgument(i) as? IrGetValue ?: continue
                        val owner = getValue.symbol.owner as? IrValueParameter ?: continue
                        if (owner.isCrossinline) {
                            expressionCapturesCrossinline = true
                            break
                        }
                    }
                    suspendLambdas += SuspendLambdaInfo(
                        expression.symbol.owner,
                        (expression.type as IrSimpleType).arguments.size - 1,
                        expression,
                        expressionCapturesCrossinline || expression in capturesCrossinline
                    )
                }
            }
        })
        return suspendLambdas
    }

    private class SuspendLambdaInfo(
        val function: IrFunction,
        val arity: Int,
        val reference: IrFunctionReference,
        val capturesCrossinline: Boolean
    ) {
        lateinit var constructor: IrConstructor
    }
}

// Transform `suspend fun foo(params): RetType` into `fun foo(params, $completion: Continuation<RetType>): Any?`
// the result is called 'view', just to be consistent with old backend.
internal fun IrFunction.getOrCreateSuspendFunctionViewIfNeeded(context: JvmBackendContext): IrFunction {
    if (!isSuspend) return this
    return context.suspendFunctionOriginalToView[suspendFunctionOriginal()] ?: suspendFunctionView(context)
}

private fun IrFunction.getOrCreateSuspendFunctionStub(context: JvmBackendContext): IrFunction {
    if (!isSuspend) return this
    return context.suspendFunctionOriginalToStub[suspendFunctionOriginal()] ?: suspendFunctionStub(context)
}

internal fun IrFunction.suspendFunctionOriginal(): IrFunction {
    require(isSuspend && this is IrSimpleFunction)
    return attributeOwnerId as IrFunction
}

private fun IrFunction.suspendFunctionStub(context: JvmBackendContext): IrFunction {
    require(this.isSuspend && this is IrSimpleFunction)
    return buildFunWithDescriptorForInlining(descriptor) {
        updateFrom(this@suspendFunctionStub)
        name = this@suspendFunctionStub.name
        origin = this@suspendFunctionStub.origin
        returnType = context.irBuiltIns.anyNType
    }.also { function ->
        function.parent = parent

        function.annotations.addAll(annotations)
        function.metadata = metadata

        function.copyAttributes(this)
        function.copyTypeParametersFrom(this)

        if (origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE) {
            function.overriddenSymbols
                .addAll(overriddenSymbols.map { it.owner.suspendFunctionViewOrStub(context).symbol as IrSimpleFunctionSymbol })
        }

        // Copy the value parameters and insert the continuation parameter. The continuation parameter
        // goes before the default argument mask(s) and handler for default argument stubs.
        // TODO: It would be nice if AddContinuationLowering could insert the continuation argument before default stub generation.
        // That would avoid the reshuffling both here and in createSuspendFunctionCallViewIfNeeded.
        function.copyValueParametersInsertingContinuationFrom(this) {
            function.addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType(context))
        }

        context.recordSuspendFunctionViewStub(this, function)
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

private fun IrFunction.suspendFunctionView(context: JvmBackendContext): IrFunction {
    require(isSuspend && this is IrSimpleFunction)
    return getOrCreateSuspendFunctionStub(context).also { function ->
        context.recordSuspendFunctionView(this, function)

        val continuationParameter =
            function.valueParameters.find { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }
                ?.let { function.valueParameters[it.index - 1] } ?: function.valueParameters.last()

        function.body =
            moveBodyTo(function, explicitParameters.zip(function.explicitParameters.filter { it != continuationParameter }).toMap())
    }
}

fun IrFunction.suspendFunctionViewOrStub(context: JvmBackendContext): IrFunction {
    if (!isSuspend) return this
    return context.suspendFunctionOriginalToView[suspendFunctionOriginal()] ?: getOrCreateSuspendFunctionStub(context)
}

private fun IrCall.createSuspendFunctionCallViewIfNeeded(context: JvmBackendContext, caller: IrFunction): IrCall {
    // Calls inside continuation are already generated with continuation parameter as well as calls to suspendImpls
    if (!isSuspend || caller.isInvokeSuspendOfContinuation()
        || symbol.owner.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION
        || (symbol.owner.valueParameters.lastOrNull()?.name?.asString() == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME)
    ) return this
    val view = (symbol.owner as IrSimpleFunction).suspendFunctionViewOrStub(context)
    if (view == symbol.owner) return this

    return IrCallImpl(startOffset, endOffset, view.returnType, view.symbol, superQualifierSymbol = superQualifierSymbol).also {
        it.copyTypeArgumentsFrom(this)
        it.copyAttributes(this)
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        val callerNumberOfMasks = caller.valueParameters.count { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }
        val callerContinuationIndex = caller.valueParameters.size - 1 - (if (callerNumberOfMasks != 0) callerNumberOfMasks + 1 else 0)
        val continuationParameter =
            when {
                caller.isInvokeSuspendOfLambda() || caller.isInvokeSuspendForInlineOfLambda() ->
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.dispatchReceiverParameter!!.symbol)
                // At this point the only LOCAL_FUNCTION_FOR_LAMBDAs are inline and crossinline lambdas
                caller.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> context.fakeContinuation
                else -> IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.valueParameters[callerContinuationIndex].symbol)
            }
        // If the suspend function view that we are calling has default parameters, we need to make sure to pass the
        // continuation before the default parameter mask(s) and handler.
        val numberOfMasks = view.valueParameters.count { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }
        val continuationIndex = valueArgumentsCount - (if (numberOfMasks != 0) numberOfMasks + 1 else 0)
        for (i in 0 until continuationIndex) {
            it.putValueArgument(i, getValueArgument(i))
        }
        it.putValueArgument(continuationIndex, continuationParameter)
        if (numberOfMasks != 0) {
            for (i in 0 until numberOfMasks + 1) {
                it.putValueArgument(valueArgumentsCount + i - 1, getValueArgument(continuationIndex + i))
            }
        }
    }
}
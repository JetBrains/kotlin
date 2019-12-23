/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
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
    "Add continuation classes to suspend functions and transform suspend lambdas into continuations"
)

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    private val functionsToAdd = mutableMapOf<IrClass, MutableSet<IrFunction>>()

    override fun lower(irFile: IrFile) {
        val (suspendLambdas, inlineLambdas) = markSuspendLambdas(irFile)
        transformSuspendFunctions(irFile, (suspendLambdas.map { it.function } + inlineLambdas).toSet())
        for ((clazz, functions) in functionsToAdd) {
            for (function in functions) {
                clazz.declarations.add(function)
            }
        }
        transformReferencesToSuspendLambdas(irFile, suspendLambdas)
    }

    private fun transformReferencesToSuspendLambdas(irFile: IrFile, suspendLambdas: List<SuspendLambdaInfo>) {
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
                        +generateContinuationClassForLambda(info, currentDeclarationParent)
                        val constructor = info.constructor
                        assert(constructor.valueParameters.size == expressionArguments.size + 1) {
                            "Inconsistency between callable reference to suspend lambda and the corresponding continuation"
                        }
                        +irCall(constructor.symbol).apply {
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

    private fun generateContinuationClassForLambda(info: SuspendLambdaInfo, parent: IrDeclarationParent): IrClass {
        val suspendLambda = context.ir.symbols.suspendLambdaClass.owner
        return suspendLambda.createContinuationClassFor(parent, JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA).apply {
            copyAttributes(info.reference)
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
                addField("\$p", it.type)
            }

            val parametersFields = info.function.valueParameters.map { addField(it.name.asString(), it.type) }
            val parametersWithoutArguments = parametersFields.withIndex()
                .mapNotNull { (i, field) -> if (info.reference.getValueArgument(i) == null) field else null }
            val parametersWithArguments = parametersFields - parametersWithoutArguments
            val constructor = addPrimaryConstructorForLambda(info.arity, info.reference, parametersFields)
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
                    parametersWithArguments,
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
        copyTypeParametersFrom(irFunction)
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

    private fun IrFunction.copyBodyFrom(oldFunction: IrFunction) {
        val mapping: Map<IrValueParameter, IrValueParameter> =
            (listOfNotNull(oldFunction.dispatchReceiverParameter, oldFunction.extensionReceiverParameter) + oldFunction.valueParameters)
                .zip(listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).toMap()
        copyBodyWithParametersMapping(this, oldFunction, mapping)
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
        parametersWithArguments: List<IrField>,
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
                    for ((i, field) in parametersWithArguments.withIndex()) {
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

    private fun IrClass.createContinuationClassFor(parent: IrDeclarationParent, newOrigin: IrDeclarationOrigin): IrClass = buildClass {
        name = Name.special("<Continuation>")
        origin = newOrigin
        visibility = JavaVisibilities.PACKAGE_VISIBILITY
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
            .createContinuationClassFor(irFunction, JvmLoweredDeclarationOrigin.CONTINUATION_CLASS)
            .apply {
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
                        it.putTypeArgument(i, context.irBuiltIns.anyNType)
                    }
                    val capturedThisValue = capturedThisField?.let { irField ->
                        irGetField(irGet(function.dispatchReceiverParameter!!), irField)
                    }
                    if (irFunction.dispatchReceiverParameter != null) {
                        it.dispatchReceiver = capturedThisValue
                    }
                    irFunction.extensionReceiverParameter?.let { extensionReceiverParameter ->
                        it.extensionReceiver = IrConstImpl.defaultValueForType(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionReceiverParameter.type
                        )
                    }
                    for ((i, parameter) in irFunction.valueParameters.withIndex()) {
                        val defaultValueForParameter = IrConstImpl.defaultValueForType(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.type
                        )
                        it.putValueArgument(i, defaultValueForParameter)
                    }
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
        copyBodyToStatic(irFunction, static)
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
                    it.putValueArgument(i++, irNull())
                }
                for (parameter in irFunction.valueParameters) {
                    val defaultValueForParameter = IrConstImpl.defaultValueForType(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.type
                    )
                    it.putValueArgument(i++, defaultValueForParameter)
                }
            })
        }
        return static
    }

    // TODO: Generate two copies of inline suspend functions
    private fun transformSuspendFunctions(irFile: IrFile, suspendAndInlineLambdas: Set<IrFunction>) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionsStack = arrayListOf<IrFunction>()
            private val suspendFunctionsCapturingCrossinline = mutableSetOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionsStack.push(declaration)
                val function = super.visitFunction(declaration) as IrFunction
                functionsStack.pop()
                if (skip(function)) return function

                function as IrSimpleFunction
                if (function in suspendFunctionsCapturingCrossinline) {
                    val newFunction = buildFun {
                        name = Name.identifier(function.name.asString() + FOR_INLINE_SUFFIX)
                        returnType = function.returnType
                        modality = function.modality
                        isSuspend = function.isSuspend
                        origin = JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
                    }.apply {
                        dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this)
                        extensionReceiverParameter = function.extensionReceiverParameter?.copyTo(this)
                        function.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
                        copyBodyFrom(function)
                        copyAttributes(function)
                    }
                    registerNewFunction(function.parentAsClass, newFunction)
                }

                val newFunction = if (function.isOverridable) {
                    // Create static method for the suspend state machine method so that reentering the method
                    // does not lead to virtual dispatch to the wrong method.
                    registerNewFunction(function.parentAsClass, function)
                    createStaticSuspendImpl(function)
                } else function

                newFunction.body = context.createIrBuilder(newFunction.symbol).irBlockBody {
                    +generateContinuationClassForNamedFunction(
                        newFunction,
                        function.dispatchReceiverParameter,
                        declaration as IrAttributeContainer
                    )
                    for (statement in newFunction.body!!.statements) {
                        +statement
                    }
                }
                return newFunction
            }

            private fun skip(function: IrFunction) =
                !function.isSuspend || function in suspendAndInlineLambdas || function.isInline || function.body == null ||
                        function.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE ||
                        function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                        function.parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

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

    private fun registerNewFunction(container: IrClass, function: IrFunction) {
        functionsToAdd.getOrPut(container) { mutableSetOf() }.add(function)
    }

    private fun markSuspendLambdas(irElement: IrElement): Pair<List<SuspendLambdaInfo>, List<IrFunction>> {
        val suspendLambdas = arrayListOf<SuspendLambdaInfo>()
        val capturesCrossinline = mutableSetOf<IrCallableReference>()
        val visitor = object : IrInlineReferenceLocator(context) {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun handleInlineFunctionCallableReferenceParam(valueArgument: IrCallableReference) {
                super.handleInlineFunctionCallableReferenceParam(valueArgument)
                val getValue = (valueArgument as? IrGetValue) ?: return
                val parameter = getValue.symbol.owner as? IrValueParameter ?: return
                if (parameter.isCrossinline) {
                    capturesCrossinline += valueArgument
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
        }
        irElement.acceptChildrenVoid(visitor)
        return suspendLambdas to visitor.inlineReferences.map { it.symbol.owner as IrFunction }
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
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrBlock
import org.jetbrains.kotlin.backend.jvm.codegen.isInvokeOfSuspendCallableReference
import org.jetbrains.kotlin.codegen.coroutines.*
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
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation classes to suspend functions and transform suspend lambdas into continuations"
)

private object CONTINUATION_CLASS : IrDeclarationOriginImpl("CONTINUATION_CLASS")

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val suspendLambdas = markSuspendLambdas(irFile)
        val suspendFunctions = markSuspendFunctions(irFile, suspendLambdas.map { it.function }.toSet())
        for (lambda in suspendLambdas) {
            generateContinuationClassForLambda(lambda)
        }
        transformReferencesToSuspendLambdas(irFile, suspendLambdas)
        transformSuspendCalls(irFile)
        for (suspendFunction in suspendFunctions) {
            generateContinuationClassForNamedFunction(suspendFunction)
        }
    }

    private fun transformReferencesToSuspendLambdas(irFile: IrFile, suspendLambdas: List<SuspendLambdaInfo>) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.isSuspend)
                    return expression
                val constructor = suspendLambdas.singleOrNull { it.function == expression.symbol.owner }?.constructor
                    ?: return expression
                val expressionArguments = expression.getArguments().map { it.second }
                assert(constructor.valueParameters.size == expressionArguments.size) {
                    "Inconsistency between callable reference to suspend lambda and the corresponding continuation"
                }
                val irBuilder = context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    return irCall(constructor.symbol).apply {
                        expressionArguments.forEachIndexed { index, argument ->
                            putValueArgument(index, argument)
                        }
                    }
                }
            }
        })
    }

    private fun generateContinuationClassForLambda(info: SuspendLambdaInfo) {
        val suspendLambda = context.ir.symbols.suspendLambdaClass.owner
        suspendLambda.createContinuationClassFor(info.function).apply {
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
            val secondaryConstructor = addSecondaryConstructorForLambda(constructor)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.valueParameters.size == info.arity + 1 && it.owner.name.asString() == "invoke"
            }
            val invokeSuspend = addInvokeSuspendForLambda(info.function, parametersFields, receiverField)
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

            context.suspendLambdaToOriginalFunctionMap[this] = info.function

            info.constructor = secondaryConstructor
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
        return addFunctionOverride(superMethod)
            .also { function ->
                function.copyTypeParametersFrom(irFunction)
                function.body = irFunction.body?.deepCopyWithSymbols(function)
                function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
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
                                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.dispatchReceiverParameter!!.symbol)
                            }
                        } else if (expression.symbol.owner == irFunction.dispatchReceiverParameter) {
                            return IrGetValueImpl(expression.startOffset, expression.endOffset, function.dispatchReceiverParameter!!.symbol)
                        }
                        val field = fields.find { it.name == expression.symbol.owner.name } ?: return expression
                        return IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, field.type).also {
                            it.receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.dispatchReceiverParameter!!.symbol)
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
                        return IrReturnImpl(ret.startOffset, ret.endOffset, ret.type, function.symbol, ret.value)
                    }
                })
                (irFunction.parent as IrDeclarationContainer).declarations.remove(irFunction)
            }
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

    private fun IrClass.createContinuationClassFor(irFunction: IrFunction): IrClass = buildClass {
        name = "${irFunction.name}\$Continuation".synthesizedName
        origin = CONTINUATION_CLASS
        visibility = JavaVisibilities.PACKAGE_VISIBILITY
    }.also { irClass ->
        irClass.createImplicitParameterDeclarationWithWrappedDescriptor()
        irClass.superTypes.add(defaultType)

        val parent = irFunction.parents.firstIsInstance<IrDeclarationContainer>()
        irClass.parent = parent
        parent.declarations.add(irClass)
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

    // Secondary constructor accepts parameters equals to arguments of function reference and is used for callable references
    // TODO: get rid of it and use primary constructor only
    private fun IrClass.addSecondaryConstructorForLambda(primary: IrConstructor): IrConstructor =
        addConstructor {
            isPrimary = false
            returnType = defaultType
        }.also { constructor ->
            constructor.valueParameters.addAll(primary.valueParameters.dropLast(1).map { it.copyTo(constructor) })

            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(primary).also {
                    for ((index, param) in constructor.valueParameters.withIndex()) {
                        it.putValueArgument(index, irGet(param))
                    }
                    it.putValueArgument(constructor.valueParameters.size, irNull())
                }
            }
        }

    private fun IrFunction.addCompletionValueParameter(): IrValueParameter =
        addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType())

    private fun IrFunction.continuationType(): IrType =
        context.ir.symbols.continuationClass.typeWith(returnType).makeNullable()

    private fun generateContinuationClassForNamedFunction(irFunction: IrFunction) {
        context.ir.symbols.continuationImplClass.owner.createContinuationClassFor(irFunction).apply {
            val resultField = addField(
                context.state.languageVersionSettings.dataFieldName(),
                context.irBuiltIns.anyType,
                JavaVisibilities.PACKAGE_VISIBILITY
            )
            val capturedThisField = irFunction.dispatchReceiverParameter?.let { addField("this\$0", it.type) }
            val labelField = addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaVisibilities.PACKAGE_VISIBILITY)
            addConstructorForNamedFunction(capturedThisField)
            var function = irFunction
            if (function is IrSimpleFunction && function.isOverridable && function.body != null) {
                // Create static method for the suspend state machine method so that reentering the method
                // does not lead to virtual dispatch to the wrong method.
                context.suspendFunctionContinuations[function] = this
                function = createStaticSuspendImpl(function)
            }
            addInvokeSuspendForNamedFunction(function, resultField, labelField, capturedThisField, function != irFunction)
            context.suspendFunctionContinuations[function] = this
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
        val invokeSuspend = context.ir.symbols.continuationImplClass.owner.functions.single { it.name == Name.identifier(INVOKE_SUSPEND_METHOD_NAME) }
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
        (irFunction.parent as IrClass).declarations.add(static)
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
    private fun markSuspendFunctions(irFile: IrFile, suspendLambdas: Set<IrFunction>): Set<IrFunction> {
        val result = hashSetOf<IrFunction>()

        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                super.visitFunction(declaration)
                if (declaration.isSuspend && declaration !in suspendLambdas && !declaration.isInline &&
                    !declaration.isInvokeOfSuspendCallableReference()
                ) {
                    result.add(declaration)
                }
            }
        })

        return result
    }

    private fun markSuspendLambdas(irElement: IrElement): List<SuspendLambdaInfo> {
        val suspendLambdas = arrayListOf<SuspendLambdaInfo>()
        val inlineLambdas = mutableSetOf<IrFunctionReference>()
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                val owner = expression.symbol.owner
                if (owner.isInline) {
                    for (i in 0 until expression.valueArgumentsCount) {
                        if (owner.valueParameters[i].isNoinline) continue

                        val valueArgument = expression.getValueArgument(i) ?: continue
                        if (valueArgument is IrBlock && valueArgument.isInlineIrBlock()) {
                            assert(valueArgument !is IrCallableReference) {
                                "callable references should be lowered to function references"
                            }
                            inlineLambdas += valueArgument.statements.filterIsInstance<IrFunctionReference>().single()
                        }
                    }
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                if (expression.isSuspend && expression !in inlineLambdas && expression.origin == IrStatementOrigin.LAMBDA) {
                    suspendLambdas += SuspendLambdaInfo(
                        expression.symbol.owner,
                        (expression.type as IrSimpleType).arguments.size - 1,
                        expression
                    )
                }
            }
        })
        return suspendLambdas
    }

    private fun transformSuspendCalls(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = mutableListOf<IrFunction>()
            override fun visitElement(element: IrElement): IrElement {
                element.transformChildrenVoid(this)
                return element
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                val res = super.visitFunction(declaration)
                functionStack.pop()
                return res
            }
        })
    }

    private class SuspendLambdaInfo(val function: IrFunction, val arity: Int, val reference: IrFunctionReference) {
        lateinit var constructor: IrConstructor
    }
}
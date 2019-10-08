/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrBlock
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.config.coroutinesPackageFqName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
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
    private val continuation by lazy {
        context.getTopLevelClass(context.state.languageVersionSettings.coroutinesPackageFqName().child(Name.identifier("Continuation")))
    }
    private val continuationImpl by lazy {
        context.getTopLevelClass(context.state.languageVersionSettings.coroutinesJvmInternalPackageFqName().child(Name.identifier("ContinuationImpl")))
    }
    private val suspendLambda by lazy {
        context.getTopLevelClass(context.state.languageVersionSettings.coroutinesJvmInternalPackageFqName().child(Name.identifier("SuspendLambda")))
    }

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
        val suspendLambda = suspendLambda.owner
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
            val constructor = addPrimaryConstructorForLambda(info.arity, info.function, parametersFields)
            val secondaryConstructor = addSecondaryConstructorForLambda(constructor)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.valueParameters.size == info.arity + 1 && it.owner.name.asString() == "invoke"
            }
            val invokeSuspend = addInvokeSuspendForLambda(info.function, parametersFields, receiverField)
            if (info.arity <= 1) {
                val create = addCreate(constructor, suspendLambda, info.arity, parametersFields, receiverField)
                addInvoke(create, invokeSuspend, invokeToOverride)
            } else {
                addInvoke(constructor, invokeSuspend, invokeToOverride)
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
        val superMethod = suspendLambda.functions.single {
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

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        val ret = super.visitReturn(expression) as IrReturn
                        return IrReturnImpl(ret.startOffset, ret.endOffset, context.irBuiltIns.anyType, function.symbol, ret.value)
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

    private fun IrClass.addInvoke(
        create: IrFunction,
        invokeSuspend: IrFunction,
        invokeToOverride: IrSimpleFunctionSymbol
    ) {
        val unitClass = context.irBuiltIns.unitClass
        val unitField = context.declarationFactory.getFieldForObjectInstance(unitClass.owner)
        addFunctionOverride(invokeToOverride.owner).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                +irReturn(irCall(invokeSuspend).also { invokeSuspendCall ->
                    invokeSuspendCall.dispatchReceiver = irCall(create).also {
                        it.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                        for ((index, param) in function.valueParameters.withIndex()) {
                            it.putValueArgument(index, irGet(param))
                        }
                    }
                    invokeSuspendCall.putValueArgument(0, irGetField(null, unitField))
                })
            }
        }
    }

    private fun IrClass.addCreate(
        constructor: IrFunction,
        superType: IrClass,
        arity: Int,
        parametersFields: List<IrField>,
        receiverField: IrField?
    ): IrFunction {
        val create = superType.functions.single {
            it.name.asString() == "create" && it.valueParameters.size == arity + 1 &&
                    it.valueParameters.last().type.isContinuation() &&
                    if (arity == 1) it.valueParameters.first().type.isNullableAny() else true
        }
        return addFunctionOverride(create).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                val constructorCall = irCall(constructor).also {
                    for ((i, field) in parametersFields.withIndex()) {
                        it.putValueArgument(i, irGetField(irGet(function.dispatchReceiverParameter!!), field))
                    }
                    it.putValueArgument(parametersFields.size, irGet(function.valueParameters.last()))
                }
                if (receiverField != null) {
                    assert(function.valueParameters.size == 2)
                    val result = irTemporary(constructorCall, "result")
                    +irSetField(irGet(result), receiverField, irGet(function.valueParameters.first()))
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

    private fun IrClass.addPrimaryConstructorForLambda(
        arity: Int,
        irFunction: IrFunction,
        fields: List<IrField>
    ): IrConstructor =
        addConstructor {
            isPrimary = true
            returnType = defaultType
        }.also { constructor ->
            irFunction.valueParameters.mapTo(constructor.valueParameters) { it.copyTo(constructor) }
            val completionParameterSymbol = constructor.addCompletionValueParameter()

            val superClassConstructor = suspendLambda.owner.constructors.single {
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

    private fun IrFunction.continuationType(): IrSimpleType =
        continuation.createType(true, listOf(makeTypeProjection(returnType, Variance.INVARIANT)))

    private fun generateContinuationClassForNamedFunction(irFunction: IrFunction) {
        continuationImpl.owner.createContinuationClassFor(irFunction).apply {
            val resultField = addField(
                context.state.languageVersionSettings.dataFieldName(),
                context.irBuiltIns.anyType,
                JavaVisibilities.PACKAGE_VISIBILITY
            )
            val capturedThisField = irFunction.dispatchReceiverParameter?.let { addField("this\$0", it.type) }
            val labelField = addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaVisibilities.PACKAGE_VISIBILITY)
            addConstructorForNamedFunction(capturedThisField)
            addInvokeSuspendForNamedFunction(irFunction, resultField, labelField, capturedThisField)

            context.suspendFunctionContinuations[irFunction] = this
        }
    }

    private fun IrClass.addConstructorForNamedFunction(capturedThisField: IrField?): IrConstructor = addConstructor {
        isPrimary = true
        returnType = defaultType
    }.also { constructor ->
        val capturedThisParameter = capturedThisField?.let { constructor.addValueParameter(it.name.asString(), it.type) }
        val completionParameterSymbol = constructor.addCompletionValueParameter()

        val superClassConstructor = continuationImpl.owner.constructors.single { it.valueParameters.size == 1 }
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
        capturedThisField: IrField?
    ) {
        val invokeSuspend = continuationImpl.owner.functions.single { it.name == Name.identifier(INVOKE_SUSPEND_METHOD_NAME) }
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
                    it.dispatchReceiver = capturedThisField?.let { irField ->
                        irGetField(irGet(function.dispatchReceiverParameter!!), irField)
                    }
                    if (irFunction.extensionReceiverParameter != null) {
                        it.extensionReceiver = irNull()
                    }
                    for (i in irFunction.valueParameters.indices) {
                        // TODO: also support primitives
                        it.putValueArgument(i, irNull())
                    }
                })
            }
        }
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
                if (declaration.isSuspend && declaration !in suspendLambdas && !declaration.isInline) {
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

                if (expression.isSuspend && expression !in inlineLambdas) {
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

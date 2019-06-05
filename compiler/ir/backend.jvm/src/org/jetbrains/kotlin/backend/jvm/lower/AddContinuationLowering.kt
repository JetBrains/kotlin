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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.psiElement
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

// TODO: Split into several lowerings, merge with ones in JS and Native
internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation parameter to suspend functions and suspend calls"
)

private object CONTINUATION_CLASS : IrDeclarationOriginImpl("CONTINUATION_CLASS")

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val suspendLambdas = markSuspendLambdas(irFile)
        val suspendFunctions = addContinuationParameter(irFile, suspendLambdas)
        suspendLambdas.forEach { generateContinuationClassForLambda(it) }
        transformReferencesToSuspendLambdas(irFile, suspendLambdas)
        transformSuspendCalls(irFile)
        for (suspendFunction in suspendFunctions) {
            generateContinuationClassForNamedFunction(suspendFunction)
        }
    }

    private fun transformReferencesToSuspendLambdas(irFile: IrFile, suspendLambdas: List<SuspendLambdaInfo>) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = Stack<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                val res = super.visitFunction(declaration)
                functionStack.pop()
                return res
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.isSuspend)
                    return expression
                val constructor = suspendLambdas.single { it.function == expression.symbol.owner }.constructor
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
        val suspendLambda = context.suspendLambda.owner
        suspendLambda.createContinuationClassFor(info.function).apply {
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

            val parametersFields = info.function.valueParameters.map { addField(it.name.asString(), it.type) }
            val constructor = addPrimaryConstructorForLambda(info.arity, info.function, parametersFields)
            val secondaryConstructor = addSecondaryConstructorForLambda(constructor)
            val invokeToOverride = functionNClass.functions.single { it.owner.valueParameters.size == info.arity + 1 }
            val invokeSuspend = addInvokeSuspendForLambda(info.function, parametersFields)
            if (info.arity <= 1) {
                val create = addCreate(constructor, suspendLambda, info.arity, parametersFields)
                addInvoke(create, invokeSuspend, invokeToOverride)
            } else {
                addInvoke(constructor, invokeSuspend, invokeToOverride)
            }

            context.suspendLambdaClasses[this] = info.function.symbol.descriptor.psiElement as KtElement

            info.constructor = secondaryConstructor
        }
    }

    private fun IrClass.addInvokeSuspendForLambda(irFunction: IrFunction, fields: List<IrField>): IrFunction {
        return addFunctionOverride(context.suspendLambda.functions.single { it.owner.name.asString() == INVOKE_SUSPEND_METHOD_NAME }.owner)
            .also { function ->
                function.body = irFunction.body?.deepCopyWithSymbols()
                function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        val field = fields.single { it.name == expression.symbol.owner.name }
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

    private fun IrClass.addInvoke(
        create: IrFunction,
        invokeSuspend: IrFunction,
        invokeToOverride: IrSimpleFunctionSymbol
    ) {
        addFunctionOverride(invokeToOverride.owner).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                +irReturn(irCall(invokeSuspend).also { invokeSuspendCall ->
                    invokeSuspendCall.dispatchReceiver = irCall(create).also {
                        it.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                        for ((index, param) in function.valueParameters.withIndex()) {
                            it.putValueArgument(index, irGet(param))
                        }
                    }
                    invokeSuspendCall.putValueArgument(0, irUnit())
                })
            }
        }
    }

    private fun IrClass.addCreate(
        constructor: IrFunction,
        superType: IrClass,
        arity: Int,
        parametersFields: List<IrField>
    ): IrFunction {
        val create = superType.functions.single { it.name == Name.identifier("create") && it.valueParameters.size == arity + 1 }
        return addFunctionOverride(create).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                +irReturn(irCall(constructor).also {
                    for ((i, field) in parametersFields.withIndex()) {
                        it.putValueArgument(i, irGetField(irGet(function.dispatchReceiverParameter!!), field))
                    }
                    for ((i, parameter) in function.valueParameters.withIndex()) {
                        it.putValueArgument(parametersFields.size + i, irGet(parameter))
                    }
                })
            }
        }
    }

    private fun IrClass.createContinuationClassFor(irFunction: IrFunction): IrClass = buildClass {
        name = "${irFunction.name}\$${context.functionReferenceAndContinuationsCount++}".synthesizedName
        origin = CONTINUATION_CLASS
        visibility = JavaVisibilities.PACKAGE_VISIBILITY
    }.also { irClass ->
        irClass.createImplicitParameterDeclarationWithWrappedDescriptor()
        irClass.superTypes.add(defaultType)

        val parent = irFunction.parents.find { it is IrDeclarationContainer } as IrDeclarationContainer
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
            constructor.valueParameters.addAll(irFunction.valueParameters.map { it.copyTo(constructor) })
            val completionParameterSymbol = constructor.addCompletionValueParameter()

            val superClassConstructor = context.suspendLambda.owner.constructors.single { it.valueParameters.size == 2 }
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
        addValueParameter(SUSPEND_FUNCTION_CONTINUATION_PARAMETER, continuationType())

    private fun IrFunction.addObjectParameter(name: String): IrValueParameter =
        addValueParameter(name, context.irBuiltIns.anyNType)

    private fun IrFunction.continuationType(): IrSimpleType =
        context.continuationClass.createType(true, listOf(makeTypeProjection(returnType, Variance.INVARIANT)))

    private fun generateContinuationClassForNamedFunction(irFunction: IrFunction) {
        context.continuationImpl.owner.createContinuationClassFor(irFunction).apply {
            val resultField = addField(
                context.state.languageVersionSettings.dataFieldName(),
                context.irBuiltIns.anyType,
                JavaVisibilities.PACKAGE_VISIBILITY
            )
            val labelField = addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaVisibilities.PACKAGE_VISIBILITY)
            addConstructorForNamedFunction()
            addInvokeSuspendForNamedFunction(irFunction, resultField, labelField)

            context.suspendFunctionContinuations[irFunction] = this
        }
    }

    private fun IrClass.addConstructorForNamedFunction(): IrConstructor = addConstructor {
        isPrimary = true
        returnType = defaultType
    }.also { constructor ->
        val completionParameterSymbol = constructor.addCompletionValueParameter()

        val superClassConstructor = context.continuationImpl.owner.constructors.single { it.valueParameters.size == 1 }
        constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(superClassConstructor).also {
                it.putValueArgument(0, irGet(completionParameterSymbol))
            }
        }
    }

    private fun IrClass.addInvokeSuspendForNamedFunction(irFunction: IrFunction, resultField: IrField, labelField: IrField) {
        val invokeSuspend = context.continuationImpl.owner.functions.single { it.name == Name.identifier(INVOKE_SUSPEND_METHOD_NAME) }
        addFunctionOverride(invokeSuspend).also { function ->
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                +irSetField(irGet(function.dispatchReceiverParameter!!), resultField, irGet(function.valueParameters[0]))
                +irSetField(
                    irGet(function.dispatchReceiverParameter!!), labelField,
                    irCallOp(
                        context.irBuiltIns.intClass.functions.single { it.owner.name == OperatorNameConventions.OR },
                        context.irBuiltIns.intType,
                        irGetField(irGet(function.dispatchReceiverParameter!!), labelField),
                        irInt(1 shl 31)
                    )
                )
                +irReturn(irCall(irFunction).also { it.putValueArgument(0, irGet(function.dispatchReceiverParameter!!)) })
            }
        }
    }

    private fun addContinuationParameter(irFile: IrFile, suspendLambdas: List<SuspendLambdaInfo>): Set<IrFunction> {
        val views = hashSetOf<IrFunction>()
        fun tryAddContinuationParameter(element: IrElement): List<IrDeclaration>? {
            return if (element is IrSimpleFunction && element.isSuspend && suspendLambdas.none { it.function == element }) {
                listOf(element.getOrCreateView().also { views.add(it) })
            } else null
        }

        irFile.transformDeclarationsFlat(::tryAddContinuationParameter)
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.transformDeclarationsFlat(::tryAddContinuationParameter)
            }
        })
        return views
    }

    private fun markSuspendLambdas(irElement: IrElement): List<SuspendLambdaInfo> {
        val suspendLambdas = arrayListOf<SuspendLambdaInfo>()
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                if (expression.isSuspend) {
                    suspendLambdas += SuspendLambdaInfo(expression.symbol.owner, expression.type.originalKotlinType!!.arguments.size - 1)
                }
            }
        })
        return suspendLambdas
    }

    private fun transformSuspendCalls(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = Stack<IrFunction>()
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

            private fun getContinuationFromCaller(): IrGetValue {
                val caller = functionStack.peek()
                val continuationParameter =
                    if (caller.isSuspend) caller.valueParameters.last().symbol
                    else if (caller.name.asString() == INVOKE_SUSPEND_METHOD_NAME && caller.parent in context.suspendLambdaClasses)
                        caller.dispatchReceiverParameter!!.symbol
                    else error("suspend call outside suspend context")
                return IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    continuationParameter
                )
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.isSuspend) return super.visitCall(expression)
                val view = (expression.symbol.owner as IrSimpleFunction).getOrCreateView()
                val res = IrCallImpl(expression.startOffset, expression.endOffset, expression.type, view.symbol).apply {
                    copyTypeArgumentsFrom(expression)
                    dispatchReceiver = expression.dispatchReceiver
                    for (i in 0 until expression.valueArgumentsCount) {
                        putValueArgument(i, expression.getValueArgument(i))
                    }
                    putValueArgument(expression.valueArgumentsCount, getContinuationFromCaller())
                }
                return super.visitCall(res)
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                if (!expression.isSuspend) return super.visitFunctionReference(expression)
                val view = (expression.symbol.owner as IrSimpleFunction).getOrCreateView()
                val res = IrFunctionReferenceImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    view.symbol,
                    view.descriptor,
                    expression.typeArgumentsCount,
                    expression.origin
                ).apply {
                    copyTypeArgumentsFrom(expression)
                    dispatchReceiver = expression.dispatchReceiver
                    for (i in 0 until expression.valueArgumentsCount) {
                        putValueArgument(i, expression.getValueArgument(i))
                    }
                }
                return super.visitFunctionReference(res)
            }
        })
    }

    private fun IrSimpleFunction.getOrCreateView(): IrSimpleFunction =
        context.transformedSuspendFunctionsCache.getOrPut(this) {
            IrFunctionImpl(
                startOffset, endOffset, origin, getOrCreateJvmSuspendFunctionView(descriptor, context.state), context.irBuiltIns.anyType
            ).also {
                it.parent = parent
                it.copyTypeParametersFrom(this)

                it.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(it)

                valueParameters.mapTo(it.valueParameters) { p -> p.copyTo(it) }
                it.addCompletionValueParameter()

                // Fix links to parameters
                val valueParametersMapping: Map<IrValueDeclaration?, IrValueParameter?> = (valueParameters + dispatchReceiverParameter)
                    .zip(it.valueParameters.dropLast(1) + it.dispatchReceiverParameter).toMap()
                it.body = body?.deepCopyWithSymbols(this)
                it.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue) =
                        valueParametersMapping[expression.symbol.owner]?.let { newParam ->
                            expression.run { IrGetValueImpl(startOffset, endOffset, type, newParam.symbol, origin) }
                        } ?: expression
                })
            }
        }

    private class SuspendLambdaInfo(val function: IrFunction, val arity: Int) {
        lateinit var constructor: IrConstructor
    }
}

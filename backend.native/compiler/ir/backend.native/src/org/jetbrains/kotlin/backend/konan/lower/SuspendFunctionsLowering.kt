/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyToWithoutSuperTypes
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWith
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

internal class SuspendFunctionsLowering(val context: Context): FileLoweringPass {

    private object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    private object DECLARATION_ORIGIN_COROUTINE_IMPL : IrDeclarationOriginImpl("COROUTINE_IMPL")

    private val builtCoroutines = mutableMapOf<IrFunction, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<IrFunction, IrFunctionReference>()

    private val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

    override fun lower(irFile: IrFile) {
        markSuspendLambdas(irFile)
        buildCoroutines(irFile)
        transformCallableReferencesToSuspendLambdas(irFile)
    }

    private fun buildCoroutines(irFile: IrFile) {
        irFile.declarations.transformFlat(::tryTransformSuspendFunction)
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.declarations.transformFlat(::tryTransformSuspendFunction)
            }
        })
    }

    private fun tryTransformSuspendFunction(element: IrElement) =
            if (element is IrSimpleFunction && element.isSuspend && element.modality != Modality.ABSTRACT)
                transformSuspendFunction(element, suspendLambdas[element])
            else null

    private fun markSuspendLambdas(irElement: IrElement) {
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                val function = expression.symbol.owner
                if (function.isSuspend)
                    suspendLambdas[function] = expression
            }
        })
    }

    private fun transformCallableReferencesToSuspendLambdas(irElement: IrElement) {
        irElement.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                val function = expression.symbol.owner
                if (!function.isSuspend)
                    return expression
                val coroutine = builtCoroutines[function]
                        ?: throw Error("The coroutine for $function has not been built")
                val constructorParameters = coroutine.coroutineConstructor.valueParameters
                val expressionArguments = expression.getArguments().map { it.second }
                assert(constructorParameters.size == expressionArguments.size)
                        { "Inconsistency between callable reference to suspend lambda and the corresponding coroutine" }
                val irBuilder = context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    return irCall(coroutine.coroutineConstructor.symbol).apply {
                        expressionArguments.forEachIndexed { index, argument ->
                            putValueArgument(index, argument)
                        }
                    }
                }
            }
        })
    }

    private sealed class SuspendFunctionKind {
        object NO_SUSPEND_CALLS : SuspendFunctionKind()
        class DELEGATING(val delegatingCall: IrCall) : SuspendFunctionKind()
        object NEEDS_STATE_MACHINE : SuspendFunctionKind()
    }

    private fun transformSuspendFunction(irFunction: IrFunction, functionReference: IrFunctionReference?): List<IrDeclaration>? {
        val suspendFunctionKind = getSuspendFunctionKind(irFunction)
        return when (suspendFunctionKind) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(
                        irFunction, suspendFunctionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val coroutine = buildCoroutine(irFunction, functionReference)   // Coroutine implementation.
                if (suspendLambdas.contains(irFunction))             // Suspend lambdas are called through factory method <create>,
                    listOf(coroutine)                                           // thus we can eliminate original body.
                else
                    listOf<IrDeclaration>(
                            coroutine,
                            irFunction
                    )
            }
        }
    }

    private fun getSuspendFunctionKind(irFunction: IrFunction): SuspendFunctionKind {
        if (suspendLambdas.contains(irFunction))
            return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

        val body = irFunction.body
                ?: return SuspendFunctionKind.NO_SUSPEND_CALLS

        var numberOfSuspendCalls = 0
        body.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)

                val callee = expression.symbol.owner
                if (callee.isSuspend)
                    ++numberOfSuspendCalls
            }
        })
        // It is important to optimize the case where there is only one suspend call and it is the last statement
        // because we don't need to build a fat coroutine class in that case.
        // This happens a lot in practise because of suspend functions with default arguments.
        // TODO: use TailRecursionCallsCollector.
        val lastStatement = (body as IrBlockBody).statements.lastOrNull()
        val lastCall = when (lastStatement) {
            is IrCall -> lastStatement
            is IrReturn -> {
                var value: IrElement = lastStatement
                /*
                 * Check if matches this pattern:
                 * block/return {
                 *     block/return {
                 *         .. suspendCall()
                 *     }
                 * }
                 */
                loop@ while (true) {
                    value = when {
                        value is IrBlock && value.statements.size == 1 -> value.statements.first()
                        value is IrReturn -> value.value
                        else -> break@loop
                    }
                }
                value as? IrCall
            }
            else -> null
        }
        val suspendCallAtEnd = lastCall != null && lastCall.symbol.owner.isSuspend     // Suspend call.
        return when {
            numberOfSuspendCalls == 0 -> SuspendFunctionKind.NO_SUSPEND_CALLS
            numberOfSuspendCalls == 1
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(lastCall!!)
            else -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val symbols = context.ir.symbols
    private val getContinuation = symbols.getContinuation.owner
    private val continuationClassSymbol = getContinuation.returnType.classifierOrFail as IrClassSymbol
    private val returnIfSuspended = symbols.returnIfSuspended

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
                if (delegatingCall.symbol == returnIfSuspended)
                    delegatingCall.getValueArgument(0)!!
                else delegatingCall
        context.createIrBuilder(irFunction.symbol).run {
            val statements = (irFunction.body as IrBlockBody).statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }
            statements[statements.size - 1] = irReturn(returnValue)
        }
    }

    private fun buildCoroutine(irFunction: IrFunction, functionReference: IrFunctionReference?): IrClass {
        val coroutine = CoroutineBuilder(irFunction, functionReference).build()
        builtCoroutines[irFunction] = coroutine

        if (functionReference == null) {
            // It is not a lambda - replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                +irReturn(
                        irCall(coroutine.invokeSuspendFunction.symbol).apply {
                            dispatchReceiver = irCall(coroutine.coroutineConstructor.symbol).apply {
                                val functionParameters = irFunction.explicitParameters
                                functionParameters.forEachIndexed { index, argument ->
                                    putValueArgument(index, irGet(argument))
                                }
                                putValueArgument(functionParameters.size,
                                        irCall(getContinuation, listOf(irFunction.returnType)))
                            }
                            putValueArgument(0, irSuccess(irGetObject(symbols.unit)))
                        })
            }
        }

        return coroutine.coroutineClass
    }

    private class BuiltCoroutine(val coroutineClass: IrClass,
                                 val coroutineConstructor: IrConstructor,
                                 val invokeSuspendFunction: IrFunction)

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val startOffset = irFunction.startOffset
        private val endOffset = irFunction.endOffset
        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private val coroutineClass = WrappedClassDescriptor().let {
            IrClassImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrClassSymbolImpl(it),
                    "${irFunction.name}\$COROUTINE\$${context.coroutineCount++}".synthesizedName,
                    ClassKind.CLASS,
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    isCompanion = false,
                    isInline = false,
                    isInner = false,
                    isData = false,
                    isExternal = false
            ).apply {
                it.bind(this)
                parent = irFunction.parent
                createParameterDeclarations()
                irFunction.typeParameters.mapTo(typeParameters) { typeParam ->
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes += typeParam.superTypes }
                }
            }
        }
        private val coroutineClassThis = coroutineClass.thisReceiver!!

        private val continuationType = continuationClassSymbol.typeWith(irFunction.returnType)

        // Save all arguments to fields.
        private val argumentToPropertiesMap = functionParameters.associate {
            it to addField(it.name, it.type, false)
        }

        private val labelField = addField(Name.identifier("label"), symbols.nativePtrType, true)

        private var tempIndex = 0
        private var suspensionPointIdIndex = 0
        private lateinit var suspendResult: IrVariable
        private lateinit var resultArgument: IrValueParameter

        private val baseClass =
                (if (irFunction.isRestrictedSuspendFunction(context.config.configuration.languageVersionSettings)) {
                    symbols.restrictedContinuationImpl
                } else {
                    symbols.continuationImpl
                }).owner

        private val baseClassConstructor = baseClass.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = baseClass.simpleFunctions()
                .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf(baseClass.defaultType)
            var suspendFunctionClass: IrClass? = null
            var functionClass: IrClass? = null
            val suspendFunctionClassTypeArguments: List<IrType>?
            val functionClassTypeArguments: List<IrType>?
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClass = symbols.suspendFunctions[numberOfParameters].owner
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.returnType
                superTypes += suspendFunctionClass.typeWith(suspendFunctionClassTypeArguments)

                functionClass = symbols.functions[numberOfParameters + 1].owner
                functionClassTypeArguments = unboundParameterTypes + continuationType + context.irBuiltIns.anyNType
                superTypes += functionClass.typeWith(functionClassTypeArguments)
            }

            val coroutineConstructor = buildConstructor()

            val superInvokeSuspendFunction = baseClass.simpleFunctions().single { it.name.asString() == "invokeSuspend" }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction, coroutineClass)

            var coroutineFactoryConstructor: IrConstructor? = null
            val createMethod: IrSimpleFunction?
            if (functionReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructor = buildFactoryConstructor(boundFunctionParameters!!)

                val createFunctionSymbol =
                        baseClass.simpleFunctions()
                                .atMostOne {
                                    it.name.asString() == "create"
                                            && it.valueParameters.size == unboundFunctionParameters!!.size + 1
                                }
                                ?.symbol
                createMethod = buildCreateMethod(
                        unboundArgs = unboundFunctionParameters!!,
                        superFunctionSymbol = createFunctionSymbol,
                        coroutineConstructor = coroutineConstructor)
                val invokeFunctionSymbol =
                        functionClass!!.simpleFunctions().single { it.name.asString() == "invoke" }.symbol
                val suspendInvokeFunctionSymbol =
                        suspendFunctionClass!!.simpleFunctions().single { it.name.asString() == "invoke" }.symbol

                buildInvokeMethod(
                        suspendFunctionInvokeFunctionSymbol = suspendInvokeFunctionSymbol,
                        functionInvokeFunctionSymbol = invokeFunctionSymbol,
                        createFunction = createMethod,
                        invokeSuspendFunction = invokeSuspendMethod)
            }

            coroutineClass.superTypes += superTypes
            coroutineClass.addFakeOverrides()

            return BuiltCoroutine(
                    coroutineClass = coroutineClass,
                    coroutineConstructor = coroutineFactoryConstructor ?: coroutineConstructor,
                    invokeSuspendFunction = invokeSuspendMethod)
        }

        private fun buildConstructor() = WrappedClassConstructorDescriptor().let {
            IrConstructorImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrConstructorSymbolImpl(it),
                    baseClassConstructor.name,
                    Visibilities.PUBLIC,
                    coroutineClass.defaultType,
                    isInline = false,
                    isExternal = false,
                    isPrimary = false
            ).apply {
                it.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                functionParameters.mapIndexedTo(valueParameters) { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }
                val continuationParameter = baseClassConstructor.valueParameters[0]
                valueParameters += continuationParameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                        index = valueParameters.size, type = continuationType)

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    val completionParameter = valueParameters.last()
                    +irDelegatingConstructorCall(baseClassConstructor).apply {
                        putValueArgument(0, irGet(completionParameter))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol, context.irBuiltIns.unitType)

                    +irSetField(irGet(coroutineClassThis), labelField, irCall(symbols.getNativeNullPtr.owner))

                    functionParameters.forEachIndexed { index, parameter ->
                        +irSetField(
                                irGet(coroutineClassThis),
                                argumentToPropertiesMap[parameter]!!,
                                irGet(valueParameters[index])
                        )
                    }
                }
            }
        }

        private fun buildFactoryConstructor(boundParams: List<IrValueParameter>) = WrappedClassConstructorDescriptor().let {
            IrConstructorImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrConstructorSymbolImpl(it),
                    baseClassConstructor.name,
                    Visibilities.PUBLIC,
                    coroutineClass.defaultType,
                    isInline = false,
                    isExternal = false,
                    isPrimary = false
            ).apply {
                it.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                boundParams.mapIndexedTo(valueParameters) { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    +irDelegatingConstructorCall(baseClassConstructor).apply {
                        putValueArgument(0, irNull()) // Completion.
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol,
                            context.irBuiltIns.unitType)
                    // Save all arguments to fields.
                    boundParams.forEachIndexed { index, parameter ->
                        +irSetField(irGet(coroutineClassThis), argumentToPropertiesMap[parameter]!!,
                                irGet(valueParameters[index]))
                    }
                }
            }
        }

        private fun buildCreateMethod(unboundArgs: List<IrValueParameter>,
                                      superFunctionSymbol: IrSimpleFunctionSymbol?,
                                      coroutineConstructor: IrConstructor) = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrSimpleFunctionSymbolImpl(it),
                    Name.identifier("create"),
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    coroutineClass.defaultType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false
            ).apply {
                it.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                (unboundArgs + create1CompletionParameter)
                        .mapIndexedTo(valueParameters) { index, parameter ->
                            parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                        }

                this.createDispatchReceiverParameter()

                superFunctionSymbol?.let { overriddenSymbols += it }

                val thisReceiver = this.dispatchReceiverParameter!!

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    +irReturn(
                            irCall(coroutineConstructor).apply {
                                var unboundIndex = 0
                                val unboundArgsSet = unboundArgs.toSet()
                                functionParameters.map {
                                    if (unboundArgsSet.contains(it))
                                        irGet(valueParameters[unboundIndex++])
                                    else
                                        irGetField(irGet(thisReceiver), argumentToPropertiesMap[it]!!)
                                }.forEachIndexed { index, argument ->
                                    putValueArgument(index, argument)
                                }
                                putValueArgument(functionParameters.size, irGet(valueParameters[unboundIndex]))
                                assert(unboundIndex == valueParameters.size - 1) {
                                    "Not all arguments of <create> are used"
                                }
                            })
                }
            }
        }

        private fun buildInvokeMethod(suspendFunctionInvokeFunctionSymbol: IrSimpleFunctionSymbol,
                                      functionInvokeFunctionSymbol: IrSimpleFunctionSymbol,
                                      createFunction: IrFunction,
                                      invokeSuspendFunction: IrFunction) = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrSimpleFunctionSymbolImpl(it),
                    Name.identifier("invoke"),
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    irFunction.returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = true
            ).apply {
                it.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                createFunction.valueParameters
                        // Skip completion - invoke() already has it implicitly as a suspend function.
                        .take(createFunction.valueParameters.size - 1)
                        .mapIndexedTo(valueParameters) { index, parameter ->
                            parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                        }

                this.createDispatchReceiverParameter()

                overriddenSymbols += functionInvokeFunctionSymbol
                overriddenSymbols += suspendFunctionInvokeFunctionSymbol

                val thisReceiver = this.dispatchReceiverParameter!!

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    +irReturn(
                            irCall(invokeSuspendFunction).apply {
                                dispatchReceiver = irCall(createFunction).apply {
                                    dispatchReceiver = irGet(thisReceiver)
                                    valueParameters.forEachIndexed { index, parameter ->
                                        putValueArgument(index, irGet(parameter))
                                    }
                                    putValueArgument(valueParameters.size,
                                            irCall(getContinuation, listOf(returnType)))
                                }
                                putValueArgument(0, irSuccess(irGetObject(symbols.unit)))
                            }
                    )
                }
            }
        }

        private fun addField(name: Name, type: IrType, isMutable: Boolean): IrField = createField(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                type,
                name,
                isMutable,
                coroutineClass
        )

        private fun buildInvokeSuspendMethod(superInvokeSuspendFunction: IrSimpleFunction,
                                             coroutineClass: IrClass): IrSimpleFunction {
            val originalBody = irFunction.body!!
            val function = WrappedSimpleFunctionDescriptor().let {
                IrFunctionImpl(
                        startOffset, endOffset,
                        DECLARATION_ORIGIN_COROUTINE_IMPL,
                        IrSimpleFunctionSymbolImpl(it),
                        superInvokeSuspendFunction.name,
                        Visibilities.PRIVATE,
                        Modality.FINAL,
                        context.irBuiltIns.anyNType,
                        isInline = false,
                        isExternal = false,
                        isTailrec = false,
                        isSuspend = false
                ).apply {
                    it.bind(this)
                    parent = coroutineClass
                    coroutineClass.declarations += this

                    superInvokeSuspendFunction.valueParameters.mapIndexedTo(valueParameters) { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                    }

                    this.createDispatchReceiverParameter()

                    overriddenSymbols += superInvokeSuspendFunction.symbol
                }
            }

            resultArgument = function.valueParameters.single()

            val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
            function.body = irBuilder.irBlockBody(startOffset, endOffset) {

                suspendResult = irVar("suspendResult".synthesizedName, context.irBuiltIns.anyNType, true)

                // Extract all suspend calls to temporaries in order to make correct jumps to them.
                originalBody.transformChildrenVoid(ExpressionSlicer(labelField.type))

                val liveLocals = computeLivenessAtSuspensionPoints(originalBody)

                val immutableLiveLocals = liveLocals.values.flatten().filterNot { it.isVar }.toSet()
                val localsMap = immutableLiveLocals.associate { it to irVar(it.name, it.type, true) }

                if (localsMap.isNotEmpty())
                    transformVariables(originalBody, localsMap)    // Make variables mutable in order to save/restore them.

                val localToPropertyMap = mutableMapOf<IrVariableSymbol, IrField>()
                // TODO: optimize by using the same property for different locals.
                liveLocals.values.forEach { scope ->
                    scope.forEach {
                        localToPropertyMap.getOrPut(it.symbol) {
                            addField(it.name, it.type, true)
                        }
                    }
                }

                originalBody.transformChildrenVoid(object : IrElementTransformerVoid() {

                    private val thisReceiver = function.dispatchReceiverParameter!!

                    // Replace returns to refer to the new function.
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid(this)

                        return if (expression.returnTargetSymbol != irFunction.symbol)
                            expression
                        else
                            irReturn(expression.value)
                    }

                    // Replace function arguments loading with properties reading.
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        expression.transformChildrenVoid(this)

                        val capturedValue = argumentToPropertiesMap[expression.symbol.owner]
                                ?: return expression
                        return irGetField(irGet(thisReceiver), capturedValue)
                    }

                    // Save/restore state at suspension points.
                    override fun visitExpression(expression: IrExpression): IrExpression {
                        expression.transformChildrenVoid(this)

                        val suspensionPoint = expression as? IrSuspensionPoint
                                ?: return expression

                        suspensionPoint.transformChildrenVoid(object : IrElementTransformerVoid() {
                            override fun visitCall(expression: IrCall): IrExpression {
                                expression.transformChildrenVoid(this)

                                when (expression.symbol) {
                                    saveState.symbol -> {
                                        val scope = liveLocals[suspensionPoint]!!
                                        return irBlock(expression) {
                                            scope.forEach {
                                                val variable = localsMap[it] ?: it
                                                +irSetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!, irGet(variable))
                                            }
                                            +irSetField(
                                                    irGet(thisReceiver),
                                                    labelField,
                                                    irGet(suspensionPoint.suspensionPointIdParameter)
                                            )
                                        }
                                    }
                                    restoreState.symbol -> {
                                        val scope = liveLocals[suspensionPoint]!!
                                        return irBlock(expression) {
                                            scope.forEach {
                                                +irSetVar(localsMap[it]
                                                        ?: it, irGetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!))
                                            }
                                        }
                                    }
                                }
                                return expression
                            }
                        })

                        return suspensionPoint
                    }
                })
                originalBody.setDeclarationsParent(function)
                val statements = (originalBody as IrBlockBody).statements
                +suspendResult
                +IrSuspendableExpressionImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        suspensionPointId = irGetField(irGet(function.dispatchReceiverParameter!!), labelField),
                        result = irBlock(startOffset, endOffset) {
                            +irThrowIfNotNull(irExceptionOrNull(irGet(resultArgument))) // Coroutine might start with an exception.
                            statements.forEach { +it }
                        })
                if (irFunction.returnType.isUnit())
                    +irReturn(irGetObject(symbols.unit))                             // Insert explicit return for Unit functions.
            }
            return function
        }

        private fun transformVariables(element: IrElement, variablesMap: Map<IrVariable, IrVariable>) {
            element.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.owner]
                            ?: return expression

                    return IrGetValueImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            type        = newVariable.type,
                            symbol      = newVariable.symbol,
                            origin      = expression.origin)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.owner]
                            ?: return expression

                    return IrSetVariableImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            type        = context.irBuiltIns.unitType,
                            symbol      = newVariable.symbol,
                            value       = expression.value,
                            origin      = expression.origin)
                }

                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val newVariable = variablesMap[declaration]
                            ?: return declaration

                    newVariable.initializer = declaration.initializer

                    return newVariable
                }
            })
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrSuspensionPoint, List<IrVariable>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrSuspensionPoint, List<IrVariable>>()
            body.acceptChildrenVoid(object: VariablesScopeTracker() {

                override fun visitExpression(expression: IrExpression) {
                    val suspensionPoint = expression as? IrSuspensionPoint
                    if (suspensionPoint == null) {
                        super.visitExpression(expression)
                        return
                    }

                    suspensionPoint.result.acceptChildrenVoid(this)
                    suspensionPoint.resumeResult.acceptChildrenVoid(this)

                    val visibleVariables = mutableListOf<IrVariable>()
                    scopeStack.forEach { visibleVariables += it }
                    result[suspensionPoint] = visibleVariables
                }
            })

            return result
        }

        // These are marker functions to split up the lowering on two parts.
        private val saveState = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    IrDeclarationOrigin.DEFINED,
                    IrSimpleFunctionSymbolImpl(it),
                    "saveState".synthesizedName,
                    Visibilities.PRIVATE,
                    Modality.ABSTRACT,
                    context.irBuiltIns.unitType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false
            ).apply {
                it.bind(this)
            }
        }

        private val restoreState = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    IrDeclarationOrigin.DEFINED,
                    IrSimpleFunctionSymbolImpl(it),
                    "restoreState".synthesizedName,
                    Visibilities.PRIVATE,
                    Modality.ABSTRACT,
                    context.irBuiltIns.unitType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false
            ).apply {
                it.bind(this)
            }
        }

        private inner class ExpressionSlicer(val suspensionPointIdType: IrType): IrElementTransformerVoid() {
            // TODO: optimize - it has square complexity.

            override fun visitSetField(expression: IrSetField): IrExpression {
                expression.transformChildrenVoid(this)

                return sliceExpression(expression)
            }

            override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
                expression.transformChildrenVoid(this)

                return sliceExpression(expression)
            }

            private fun sliceExpression(expression: IrExpression): IrExpression {
                val irBuilder = context.createIrBuilder(irFunction.symbol, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    val children = when (expression) {
                        is IrSetField -> listOf(expression.receiver, expression.value)
                        is IrMemberAccessExpression -> (
                                listOf(expression.dispatchReceiver, expression.extensionReceiver)
                                        + (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }
                                )
                        else -> throw Error("Unexpected expression: $expression")
                    }

                    val numberOfChildren = children.size

                    val hasSuspendCallInTail = BooleanArray(numberOfChildren + 1)
                    for (i in numberOfChildren - 1 downTo 0)
                        hasSuspendCallInTail[i] = hasSuspendCallInTail[i + 1] || children[i].let { it != null && it.hasSuspendCalls() }

                    val newChildren = arrayOfNulls<IrExpression?>(numberOfChildren)
                    val tempStatements = mutableListOf<IrStatement>()
                    var first = true
                    for ((index, child) in children.withIndex()) {
                        if (child == null) continue
                        val transformedChild =
                                if (!child.isSpecialBlock())
                                    child
                                else {
                                    val statements = (child as IrBlock).statements
                                    tempStatements += statements.take(statements.size - 1)
                                    statements.last() as IrExpression
                                }
                        if (first && !hasSuspendCallInTail[index + 1]) {
                            // Don't extract suspend call to a temporary if it is the first argument and is the only suspend call.
                            newChildren[index] = transformedChild
                            first = false
                            continue
                        }
                        first = false
                        if (transformedChild.isPure() || !hasSuspendCallInTail[index])
                            newChildren[index] = transformedChild
                        else {
                            // Save to temporary in order to save execution order.
                            val tmp = irVar(transformedChild)

                            tempStatements += tmp
                            newChildren[index] = irGet(tmp)
                        }
                    }

                    var calledSaveState = false
                    var suspendCall: IrExpression? = null
                    when {
                        expression.isReturnIfSuspendedCall -> {
                            calledSaveState = true
                            val firstArgument = newChildren[2]!!
                            newChildren[2] = irBlock(firstArgument) {
                                +irCall(saveState)
                                +firstArgument
                            }
                            suspendCall = newChildren[2]
                        }
                        expression.isSuspendCall -> {
                            val lastChild = newChildren.last()
                            if (lastChild != null) {
                                // Save state as late as possible.
                                calledSaveState = true
                                newChildren[numberOfChildren - 1] =
                                        irBlock(lastChild) {
                                            if (lastChild.isPure()) {
                                                +irCall(saveState)
                                                +lastChild
                                            } else {
                                                val tmp = irVar(lastChild)
                                                +tmp
                                                +irCall(saveState)
                                                +irGet(tmp)
                                            }
                                        }
                            }
                            suspendCall = expression
                        }
                    }

                    when (expression) {
                        is IrSetField -> {
                            expression.receiver = newChildren[0]
                            expression.value = newChildren[1]!!
                        }
                        is IrMemberAccessExpression -> {
                            expression.dispatchReceiver = newChildren[0]
                            expression.extensionReceiver = newChildren[1]
                            newChildren.drop(2).forEachIndexed { index, newChild ->
                                expression.putValueArgument(index, newChild)
                            }
                        }
                    }

                    if (suspendCall == null)
                        return irWrap(expression, tempStatements)

                    val suspensionPoint = IrSuspensionPointImpl(
                            startOffset                = startOffset,
                            endOffset                  = endOffset,
                            type                       = context.irBuiltIns.anyNType,
                            suspensionPointIdParameter = irVar(
                                    "suspensionPointId${suspensionPointIdIndex++}".synthesizedName,
                                    suspensionPointIdType
                            ),
                            result                     = irBlock(startOffset, endOffset) {
                                if (!calledSaveState)
                                    +irCall(saveState)
                                +irSetVar(suspendResult.symbol, suspendCall)
                                +irReturnIfSuspended(suspendResult)
                                +irGet(suspendResult)
                            },
                            resumeResult               = irBlock(startOffset, endOffset) {
                                +irCall(restoreState)
                                +irGetOrThrow(irGet(resultArgument))
                            })
                    val expressionResult = when {
                        suspendCall.type.isUnit() -> irImplicitCoercionToUnit(suspensionPoint)
                        else -> irAs(suspensionPoint, suspendCall.type)
                    }
                    return irBlock(expression) {
                        tempStatements.forEach { +it }
                        +expressionResult
                    }
                }

            }

            private fun IrBuilderWithScope.irWrap(expression: IrExpression, tempStatements: List<IrStatement>)
                    = if (tempStatements.isEmpty())
                          expression
                      else irBlock(expression, STATEMENT_ORIGIN_COROUTINE_IMPL) {
                          tempStatements.forEach { +it }
                          +expression
                      }

            private val IrExpression.isSuspendCall: Boolean
                get() = this is IrCall && this.symbol.owner.isSuspend

            private fun IrElement.isSpecialBlock()
                    = this is IrBlock && this.origin == STATEMENT_ORIGIN_COROUTINE_IMPL

            private fun IrElement.hasSuspendCalls(): Boolean {
                var hasSuspendCalls = false
                acceptVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitCall(expression: IrCall) {
                        expression.acceptChildrenVoid(this)
                        hasSuspendCalls = hasSuspendCalls || expression.isSuspendCall
                    }

                    override fun visitExpression(expression: IrExpression) {
                        expression.acceptChildrenVoid(this)
                        hasSuspendCalls = hasSuspendCalls || expression is IrSuspensionPointImpl
                    }
                })

                return hasSuspendCalls
            }

            private fun IrExpression.isPure(): Boolean {
                return when (this) {
                    is IrConst<*> -> true
                    is IrCall -> false // TODO: skip builtin operators.
                    is IrTypeOperatorCall -> this.argument.isPure() && this.operator != IrTypeOperator.CAST
                    is IrGetValue -> !this.symbol.owner.let { it is IrVariable && it.isVar }
                    else -> false
                }
            }

            private val IrExpression.isReturnIfSuspendedCall: Boolean
                get() = this is IrCall && this.symbol == returnIfSuspended
        }

        private fun IrBuilderWithScope.irVar(initializer: IrExpression) =
                irVar("tmp${tempIndex++}".synthesizedName, initializer.type, false, initializer)

        private fun IrBuilderWithScope.irVar(name: Name, type: IrType,
                                             isMutable: Boolean = false,
                                             initializer: IrExpression? = null) = WrappedVariableDescriptor().let {
            IrVariableImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrVariableSymbolImpl(it),
                    name,
                    type,
                    isMutable,
                    isConst = false,
                    isLateinit = false
            ).apply {
                it.bind(this)
                this.initializer = initializer
                this.parent = this@irVar.parent
            }
        }

        private fun IrBuilderWithScope.irReturnIfSuspended(value: IrValueDeclaration) =
                irIfThen(irEqeqeq(irGet(value), irCall(symbols.coroutineSuspendedGetter)),
                        irReturn(irGet(value)))

        private fun IrBuilderWithScope.irThrowIfNotNull(exception: IrExpression) = irLetS(exception) {
            irThrowIfNotNull(it.owner)
        }

        fun IrBuilderWithScope.irThrowIfNotNull(exception: IrValueDeclaration) =
                irIfThen(irNot(irEqeqeq(irGet(exception), irNull())),
                        irThrow(irImplicitCast(irGet(exception), exception.type.makeNotNull())))

        fun IrBuilderWithScope.irDebugOutput(value: IrExpression) =
                irCall(symbols.println).apply {
                    putValueArgument(0, irCall(symbols.anyNToString).apply {
                        extensionReceiver = value
                    })
                }
    }

    private fun IrBuilderWithScope.irGetOrThrow(result: IrExpression): IrExpression =
            irCall(symbols.kotlinResultGetOrThrow.owner).apply {
                extensionReceiver = result
            } // TODO: consider inlining getOrThrow function body here.

    private fun IrBuilderWithScope.irExceptionOrNull(result: IrExpression): IrExpression {
        val resultClass = symbols.kotlinResult.owner
        val exceptionOrNull = resultClass.simpleFunctions().single { it.name.asString() == "exceptionOrNull" }
        return irCall(exceptionOrNull).apply {
            dispatchReceiver = result
        }
    }

    fun IrBlockBodyBuilder.irSuccess(value: IrExpression): IrCall {
        val createResult = symbols.kotlinResult.owner.constructors.single { it.isPrimary }
        return irCall(createResult).apply {
            putValueArgument(0, value)
        }
    }

    private open class VariablesScopeTracker: IrElementVisitorVoid {

        protected val scopeStack = mutableListOf<MutableSet<IrVariable>>(mutableSetOf())

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitContainerExpression(expression: IrContainerExpression) {
            if (!expression.isTransparentScope)
                scopeStack.push(mutableSetOf())
            super.visitContainerExpression(expression)
            if (!expression.isTransparentScope)
                scopeStack.pop()
        }

        override fun visitCatch(aCatch: IrCatch) {
            scopeStack.push(mutableSetOf())
            super.visitCatch(aCatch)
            scopeStack.pop()
        }

        override fun visitVariable(declaration: IrVariable) {
            super.visitVariable(declaration)
            scopeStack.peek()!!.add(declaration)
        }
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {

    protected object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    protected object DECLARATION_ORIGIN_COROUTINE_IMPL : IrDeclarationOriginImpl("COROUTINE_IMPL")

    protected abstract val stateMachineMethodName: Name
    protected abstract fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol
    protected abstract fun nameForCoroutineClass(function: IrFunction): Name

    protected abstract fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>
    )

    protected abstract fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression)

    protected abstract fun initializeStateMachine(coroutineConstructors: List<IrConstructor>, coroutineClassThis: IrValueDeclaration)

    protected open fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression =
        delegatingCall

    private val builtCoroutines = mutableMapOf<IrFunction, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<IrFunction, IrFunctionReference>()

    override fun lower(irFile: IrFile) {
        markSuspendLambdas(irFile)
        buildCoroutines(irFile)
        transformCallableReferencesToSuspendLambdas(irFile)
    }

    private fun buildCoroutines(irFile: IrFile) {
        irFile.transformDeclarationsFlat(::tryTransformSuspendFunction)
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.transformDeclarationsFlat(::tryTransformSuspendFunction)
            }
        })
    }


    // Suppress since it is used in native
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun IrCall.isReturnIfSuspendedCall() =
        symbol.owner.run { fqNameWhenAvailable == context.internalPackageFqn.child(Name.identifier("returnIfSuspended")) }

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

                if (expression.isSuspend) {
                    suspendLambdas[expression.symbol.owner] = expression
                }
            }
        })
    }

    private fun transformCallableReferencesToSuspendLambdas(irElement: IrElement) {
        irElement.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.isSuspend)
                    return expression
                val coroutine = builtCoroutines[expression.symbol.owner]
                    ?: throw Error("Non-local callable reference to suspend lambda: $expression")
                val constructorParameters = coroutine.coroutineConstructor.valueParameters
                val expressionArguments = expression.getArguments().map { it.second }
                assert(constructorParameters.size == expressionArguments.size) {
                    "Inconsistency between callable reference to suspend lambda and the corresponding coroutine"
                }
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

    private fun transformSuspendFunction(irFunction: IrSimpleFunction, functionReference: IrFunctionReference?) =
        when (val suspendFunctionKind = getSuspendFunctionKind(irFunction)) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction, suspendFunctionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val coroutine = buildCoroutine(irFunction, functionReference)   // Coroutine implementation.
                if (irFunction in suspendLambdas)             // Suspend lambdas are called through factory method <create>,
                    listOf(coroutine)                                           // thus we can eliminate original body.
                else
                    listOf<IrDeclaration>(coroutine, irFunction)
            }
        }

    private fun getSuspendFunctionKind(irFunction: IrSimpleFunction): SuspendFunctionKind {
        if (irFunction in suspendLambdas)
            return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

        val body = irFunction.body ?: return SuspendFunctionKind.NO_SUSPEND_CALLS

        var numberOfSuspendCalls = 0
        body.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)
                if (expression.isSuspend)
                    ++numberOfSuspendCalls
            }
        })
        // It is important to optimize the case where there is only one suspend call and it is the last statement
        // because we don't need to build a fat coroutine class in that case.
        // This happens a lot in practise because of suspend functions with default arguments.
        // TODO: use TailRecursionCallsCollector.
        val lastCall = when (val lastStatement = (body as IrBlockBody).statements.lastOrNull()) {
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
        val suspendCallAtEnd = lastCall != null && lastCall.isSuspend    // Suspend call.
        return when {
            numberOfSuspendCalls == 0 -> SuspendFunctionKind.NO_SUSPEND_CALLS
            numberOfSuspendCalls == 1
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(lastCall!!)
            else -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val symbols = context.ir.symbols
    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
            if (delegatingCall.isReturnIfSuspendedCall())
                delegatingCall.getValueArgument(0)!!
            else delegatingCall
        context.createIrBuilder(irFunction.symbol).run {
            val statements = (irFunction.body as IrBlockBody).statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }
            statements[statements.lastIndex] = irReturn(generateDelegatedCall(irFunction.returnType, returnValue))
        }
    }

    private fun buildCoroutine(irFunction: IrSimpleFunction, functionReference: IrFunctionReference?): IrClass {
        val coroutine = CoroutineBuilder(irFunction, functionReference).build()
        builtCoroutines[irFunction] = coroutine

        if (functionReference == null) {
            // It is not a lambda - replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                val constructor = coroutine.coroutineConstructor
                generateCoroutineStart(coroutine.stateMachineFunction,
                                       irCallConstructor(constructor.symbol, irFunction.typeParameters.map {
                                           IrSimpleTypeImpl(it.symbol, true, emptyList(), emptyList())
                                       }).apply {
                                           val functionParameters = irFunction.explicitParameters
                                           functionParameters.forEachIndexed { index, argument ->
                                               putValueArgument(index, irGet(argument))
                                           }
                                           putValueArgument(
                                               functionParameters.size,
                                               irCall(
                                                   getContinuationSymbol,
                                                   getContinuationSymbol.owner.returnType,
                                                   listOf(irFunction.returnType)
                                               )
                                           )
                                       })
            }
        }

        return coroutine.coroutineClass
    }

    private class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val stateMachineFunction: IrFunction
    )

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val startOffset = irFunction.startOffset
        private val endOffset = irFunction.endOffset
        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private val coroutineClass: IrClass = WrappedClassDescriptor().let { d ->
            IrClassImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrClassSymbolImpl(d),
                nameForCoroutineClass(irFunction),
                ClassKind.CLASS,
                irFunction.visibility,
                Modality.FINAL,
                isCompanion = false,
                isInner = false,
                isData = false,
                isExternal = false,
                isInline = false,
                isExpect = false
            ).apply {
                d.bind(this)
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
        private val argumentToPropertiesMap = functionParameters.associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(irFunction)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = coroutineBaseClass.owner.simpleFunctions()
            .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private val coroutineConstructors = mutableListOf<IrConstructor>()

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf(coroutineBaseClass.defaultType)
            var suspendFunctionClass: IrClass? = null
            var functionClass: IrClass? = null
            val suspendFunctionClassTypeArguments: List<IrType>?
            val functionClassTypeArguments: List<IrType>?
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClass = symbols.suspendFunctionN(numberOfParameters).owner
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.returnType
                superTypes += suspendFunctionClass.typeWith(suspendFunctionClassTypeArguments)

                functionClass = symbols.functionN(numberOfParameters + 1).owner
                functionClassTypeArguments = unboundParameterTypes + continuationType + context.irBuiltIns.anyNType
                superTypes += functionClass.typeWith(functionClassTypeArguments)
            }

            val coroutineConstructor = buildConstructor()

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction, coroutineClass)

            var coroutineFactoryConstructor: IrConstructor? = null
            val createMethod: IrSimpleFunction?
            if (functionReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructor = buildFactoryConstructor(boundFunctionParameters!!)

                val createFunctionSymbol = coroutineBaseClass.owner.simpleFunctions()
                    .atMostOne { it.name.asString() == "create" && it.valueParameters.size == unboundFunctionParameters!!.size + 1 }
                    ?.symbol

                createMethod = buildCreateMethod(
                    unboundArgs = unboundFunctionParameters!!,
                    superFunctionSymbol = createFunctionSymbol,
                    coroutineConstructor = coroutineConstructor
                )

                val invokeFunctionSymbol =
                    functionClass!!.simpleFunctions().single { it.name.asString() == "invoke" }.symbol
                val suspendInvokeFunctionSymbol =
                    suspendFunctionClass!!.simpleFunctions().single { it.name.asString() == "invoke" }.symbol

                buildInvokeMethod(
                    suspendFunctionInvokeFunctionSymbol = suspendInvokeFunctionSymbol,
                    functionInvokeFunctionSymbol = invokeFunctionSymbol,
                    createFunction = createMethod,
                    stateMachineFunction = invokeSuspendMethod
                )
            }

            coroutineClass.superTypes += superTypes
            coroutineClass.addFakeOverrides()

            initializeStateMachine(coroutineConstructors, coroutineClassThis)

            return BuiltCoroutine(
                coroutineClass = coroutineClass,
                coroutineConstructor = coroutineFactoryConstructor ?: coroutineConstructor,
                stateMachineFunction = invokeSuspendMethod
            )
        }

        fun buildConstructor(): IrConstructor = WrappedClassConstructorDescriptor().let { d ->
            IrConstructorImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrConstructorSymbolImpl(d),
                coroutineBaseClassConstructor.name,
                irFunction.visibility,
                coroutineClass.defaultType,
                isInline = false,
                isExternal = false,
                isPrimary = true,
                isExpect = false
            ).apply {
                d.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this
                coroutineConstructors += this

                functionParameters.mapIndexedTo(valueParameters) { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }
                val continuationParameter = coroutineBaseClassConstructor.valueParameters[0]
                valueParameters += continuationParameter.copyTo(
                    this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                    index = valueParameters.size, type = continuationType
                )

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    val completionParameter = valueParameters.last()
                    +irDelegatingConstructorCall(coroutineBaseClassConstructor).apply {
                        putValueArgument(0, irGet(completionParameter))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol, context.irBuiltIns.unitType)

                    functionParameters.forEachIndexed { index, parameter ->
                        +irSetField(
                            irGet(coroutineClassThis),
                            argumentToPropertiesMap.getValue(parameter),
                            irGet(valueParameters[index])
                        )
                    }
                }
            }
        }

        private fun buildFactoryConstructor(boundParams: List<IrValueParameter>) = WrappedClassConstructorDescriptor().let { d ->
            IrConstructorImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrConstructorSymbolImpl(d),
                coroutineBaseClassConstructor.name,
                irFunction.visibility,
                coroutineClass.defaultType,
                isInline = false,
                isExternal = false,
                isPrimary = false,
                isExpect = false
            ).apply {
                d.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this
                coroutineConstructors += this

                boundParams.mapIndexedTo(valueParameters) { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    +irDelegatingConstructorCall(coroutineBaseClassConstructor).apply {
                        putValueArgument(0, irNull()) // Completion.
                    }
                    +IrInstanceInitializerCallImpl(
                        startOffset, endOffset, coroutineClass.symbol,
                        context.irBuiltIns.unitType
                    )
                    // Save all arguments to fields.
                    boundParams.forEachIndexed { index, parameter ->
                        +irSetField(
                            irGet(coroutineClassThis), argumentToPropertiesMap.getValue(parameter),
                            irGet(valueParameters[index])
                        )
                    }
                }
            }
        }

        private fun buildCreateMethod(
            unboundArgs: List<IrValueParameter>,
            superFunctionSymbol: IrSimpleFunctionSymbol?,
            coroutineConstructor: IrConstructor
        ) = WrappedSimpleFunctionDescriptor().let { d ->
            IrFunctionImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrSimpleFunctionSymbolImpl(d),
                Name.identifier("create"),
                Visibilities.PROTECTED,
                Modality.FINAL,
                coroutineClass.defaultType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false
            ).apply {
                d.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                irFunction.typeParameters.mapTo(typeParameters) { parameter ->
                    parameter.copyToWithoutSuperTypes(this, 0, DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes += parameter.superTypes }
                }

                (unboundArgs + create1CompletionParameter)
                    .mapIndexedTo(valueParameters) { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                    }

                this.createDispatchReceiverParameter()

                superFunctionSymbol?.let {
                    overriddenSymbols += it.owner.overriddenSymbols
                    overriddenSymbols += it
                }

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
                                    irGetField(irGet(thisReceiver), argumentToPropertiesMap.getValue(it))
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

        private fun buildInvokeMethod(
            suspendFunctionInvokeFunctionSymbol: IrSimpleFunctionSymbol,
            functionInvokeFunctionSymbol: IrSimpleFunctionSymbol,
            createFunction: IrFunction,
            stateMachineFunction: IrFunction
        ) = WrappedSimpleFunctionDescriptor().let { d ->
            IrFunctionImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrSimpleFunctionSymbolImpl(d),
                Name.identifier("invoke"),
                Visibilities.PROTECTED,
                Modality.FINAL,
                context.irBuiltIns.anyNType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = true,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false
            ).apply {
                d.bind(this)
                parent = coroutineClass
                coroutineClass.declarations += this

                irFunction.typeParameters.mapTo(typeParameters) { parameter ->
                    parameter.copyToWithoutSuperTypes(this, 0, DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes += parameter.superTypes }
                }

                createFunction.valueParameters
                    // Skip completion - invoke() already has it implicitly as a suspend function.
                    .take(createFunction.valueParameters.size - 1)
                    .mapIndexedTo(valueParameters) { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                    }

                this.createDispatchReceiverParameter()

                overriddenSymbols += functionInvokeFunctionSymbol
                overriddenSymbols += suspendFunctionInvokeFunctionSymbol

                val thisReceiver = dispatchReceiverParameter!!

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    generateCoroutineStart(stateMachineFunction, irCall(createFunction).apply {
                        dispatchReceiver = irGet(thisReceiver)
                        valueParameters.forEachIndexed { index, parameter ->
                            putValueArgument(index, irGet(parameter))
                        }
                        putValueArgument(
                            valueParameters.size,
                            irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(returnType))
                        )
                    })
                }
            }
        }

        private fun buildInvokeSuspendMethod(
            stateMachineFunction: IrSimpleFunction,
            coroutineClass: IrClass
        ): IrSimpleFunction {
            val function = WrappedSimpleFunctionDescriptor().let { d ->
                IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrSimpleFunctionSymbolImpl(d),
                    stateMachineFunction.name,
                    stateMachineFunction.visibility,
                    Modality.FINAL,
                    context.irBuiltIns.anyNType,
                    isInline = stateMachineFunction.isInline,
                    isExternal = stateMachineFunction.isExternal,
                    isTailrec = stateMachineFunction.isTailrec,
                    isSuspend = stateMachineFunction.isSuspend,
                    isExpect = stateMachineFunction.isExpect,
                    isFakeOverride = false,
                    isOperator = false
                ).apply {
                    d.bind(this)
                    parent = coroutineClass
                    coroutineClass.declarations += this

                    stateMachineFunction.typeParameters.mapTo(typeParameters) { parameter ->
                        parameter.copyToWithoutSuperTypes(this, 0, DECLARATION_ORIGIN_COROUTINE_IMPL)
                            .apply { superTypes += parameter.superTypes }
                    }

                    stateMachineFunction.valueParameters.mapIndexedTo(valueParameters) { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                    }

                    this.createDispatchReceiverParameter()

                    overriddenSymbols += stateMachineFunction.symbol
                }
            }

            buildStateMachine(function, irFunction, argumentToPropertiesMap)
            return function
        }
    }

    protected open class VariablesScopeTracker : IrElementVisitorVoid {

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

    fun IrClass.addField(name: Name, type: IrType, isMutable: Boolean): IrField {
        val descriptor = WrappedFieldDescriptor()
        val symbol = IrFieldSymbolImpl(descriptor)
        return IrFieldImpl(
            startOffset,
            endOffset,
            DECLARATION_ORIGIN_COROUTINE_IMPL,
            symbol,
            name,
            type,
            Visibilities.PRIVATE,
            isFinal = !isMutable,
            isExternal = false,
            isStatic = false,
            isFakeOverride = false
        ).also {
            descriptor.bind(it)
            it.parent = this
            addChild(it)
        }
    }
}

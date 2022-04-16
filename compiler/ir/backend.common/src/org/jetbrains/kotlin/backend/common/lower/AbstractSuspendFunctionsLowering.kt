/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {

    protected object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    protected object DECLARATION_ORIGIN_COROUTINE_IMPL : IrDeclarationOriginImpl("COROUTINE_IMPL")

    protected abstract val stateMachineMethodName: Name
    protected abstract fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol
    protected abstract fun nameForCoroutineClass(function: IrFunction): Name
    protected abstract fun IrBuilderWithScope.launchSuspendFunctionWithGivenContinuation(
        symbol: IrSimpleFunctionSymbol, dispatchReceiver: IrExpression,
        arguments: List<IrExpression>, continuation: IrExpression) : IrExpression

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
        addMissingSupertypesToSuspendFunctionImplementingClasses(irFile)
    }

    private fun addMissingSupertypesToSuspendFunctionImplementingClasses(irFile: IrFile) {
        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                // Don't need to iterate through children. All local classes are already moved to the top level by this moment.
            }

            override fun visitClass(declaration: IrClass) {
                addMissingSupertypes(declaration)
                declaration.acceptChildrenVoid(this)
            }


            private fun addMissingSupertypes(clazz: IrClass) {
                val suspendFunctionTypes = getAllSubstitutedSupertypes(clazz).filter {
                    // SuspendFunction class is some hack in old Kotlin/Native compiler versions.
                    // It's not used now, but is considered as SuspendFunction-like class in isSuspendFunction util,
                    // if found in old klib. We need just to ignore it.
                    it.isSuspendFunction() && it.classOrNull?.owner?.name?.toString() != "SuspendFunction"
                }.toSet()

                for (suspendFunctionType in suspendFunctionTypes) {
                    val suspendFunctionClassSymbol = suspendFunctionType.classOrNull ?: continue
                    val suspendFunction = suspendFunctionClassSymbol.owner.simpleFunctions().single {
                        it.name == OperatorNameConventions.INVOKE
                    }

                    val invokeFunction = clazz.simpleFunctions().single {
                        it.name == OperatorNameConventions.INVOKE && it.overrides(suspendFunction)
                    }

                    if (invokeFunction.modality == Modality.ABSTRACT) {
                        continue
                    }

                    val suspendFunctionArity = suspendFunction.valueParameters.size
                    val functionClassSymbol = symbols.functionN(suspendFunctionArity + 1)
                    val functionSymbol = functionClassSymbol.owner.simpleFunctions().single {
                        it.name == OperatorNameConventions.INVOKE
                    }.symbol

                    val functionClassTypeArguments = suspendFunctionType.arguments.mapIndexed { index, argument ->
                        val type = (argument as IrTypeProjection).type
                        if (index == suspendFunctionArity) continuationClassSymbol.typeWith(type) else type
                    } + context.irBuiltIns.anyNType

                    val functionType = functionClassSymbol.typeWith(functionClassTypeArguments)

                    clazz.superTypes += functionType

                    context.irFactory.buildFun {
                        startOffset = SYNTHETIC_OFFSET
                        endOffset = SYNTHETIC_OFFSET
                        origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                        name = OperatorNameConventions.INVOKE
                        visibility = DescriptorVisibilities.PROTECTED
                        returnType = context.irBuiltIns.anyNType
                    }.apply {
                        parent = clazz
                        clazz.declarations += this

                        typeParameters = invokeFunction.typeParameters.map { parameter ->
                            parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                                .apply { superTypes += parameter.superTypes }
                        }

                        valueParameters = invokeFunction.valueParameters
                            .mapIndexed { index, parameter ->
                                parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                            }
                        valueParameters += buildValueParameter(this) {
                            index = valueParameters.size
                            type = context.irBuiltIns.anyNType
                            name = "completion".synthesizedName
                        }

                        this.createDispatchReceiverParameter()

                        overriddenSymbols += functionSymbol

                        val thisReceiver = dispatchReceiverParameter!!

                        val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                        body = irBuilder.irBlockBody(startOffset, endOffset) {
                            +irReturn(
                                launchSuspendFunctionWithGivenContinuation(
                                    invokeFunction.symbol,
                                    irGet(thisReceiver),
                                    valueParameters.dropLast(1).map { irGet(it) },
                                    irGet(valueParameters.last())
                                )
                            )
                        }
                    }
                }
            }
        })
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
        symbol.signature == context.ir.symbols.returnIfSuspended.signature

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
                val expressionArguments = expression.getArgumentsWithIr().map { it.second }
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
                                           it.defaultType.makeNullable()
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
        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private val coroutineClass: IrClass =
            context.irFactory.buildClass {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(irFunction)
                visibility = irFunction.visibility
            }.apply {
                parent = irFunction.parent
                createParameterDeclarations()
                typeParameters = irFunction.typeParameters.map { typeParam ->
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes += typeParam.superTypes }
                }
            }

        private val coroutineClassThis = coroutineClass.thisReceiver!!

        private val continuationType = continuationClassSymbol.typeWith(irFunction.returnType)

        // Save all arguments to fields.
        private val argumentToPropertiesMap = functionParameters.associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(irFunction)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = coroutineBaseClass.owner.simpleFunctions()
            .single { it.name == CREATE_IDENTIFIER && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private val coroutineConstructors = mutableListOf<IrConstructor>()

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf(coroutineBaseClass.defaultType)
            var suspendFunctionClass: IrClass? = null
            val suspendFunctionClassTypeArguments: List<IrType>?
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClass = symbols.suspendFunctionN(numberOfParameters).owner
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.returnType
                superTypes += suspendFunctionClass.typeWith(suspendFunctionClassTypeArguments)
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
                    .atMostOne { it.name == CREATE_IDENTIFIER && it.valueParameters.size == unboundFunctionParameters!!.size + 1 }
                    ?.symbol

                createMethod = buildCreateMethod(
                    unboundArgs = unboundFunctionParameters!!,
                    superFunctionSymbol = createFunctionSymbol,
                    coroutineConstructor = coroutineConstructor
                )

                val suspendInvokeFunction =
                    suspendFunctionClass!!.simpleFunctions().single { it.name == OperatorNameConventions.INVOKE }

                buildInvokeMethod(
                    functionInvokeFunction = suspendInvokeFunction,
                    createFunction = createMethod,
                    stateMachineFunction = invokeSuspendMethod,
                )
            }

            coroutineClass.superTypes += superTypes
            coroutineClass.addFakeOverrides(context.typeSystem)

            initializeStateMachine(coroutineConstructors, coroutineClassThis)

            return BuiltCoroutine(
                coroutineClass = coroutineClass,
                coroutineConstructor = coroutineFactoryConstructor ?: coroutineConstructor,
                stateMachineFunction = invokeSuspendMethod
            )
        }

        fun buildConstructor(): IrConstructor =
            context.irFactory.buildConstructor {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                visibility = irFunction.visibility
                returnType = coroutineClass.defaultType
                isPrimary = true
            }.apply {
                parent = coroutineClass
                coroutineClass.declarations += this
                coroutineConstructors += this

                valueParameters = functionParameters.mapIndexed { index, parameter ->
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

        private fun buildFactoryConstructor(boundParams: List<IrValueParameter>): IrConstructor =
            context.irFactory.buildConstructor {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                visibility = irFunction.visibility
                returnType = coroutineClass.defaultType
            }.apply {
                parent = coroutineClass
                coroutineClass.declarations += this
                coroutineConstructors += this

                valueParameters = boundParams.mapIndexed { index, parameter ->
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

        private fun buildCreateMethod(
            unboundArgs: List<IrValueParameter>,
            superFunctionSymbol: IrSimpleFunctionSymbol?,
            coroutineConstructor: IrConstructor
        ): IrSimpleFunction = context.irFactory.buildFun {
            startOffset = irFunction.startOffset
            endOffset = irFunction.endOffset
            origin = DECLARATION_ORIGIN_COROUTINE_IMPL
            name = CREATE_IDENTIFIER
            visibility = DescriptorVisibilities.PROTECTED
            returnType = coroutineClass.defaultType
        }.apply {
            parent = coroutineClass
            coroutineClass.declarations += this

            typeParameters = irFunction.typeParameters.map { parameter ->
                parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                    .apply { superTypes += parameter.superTypes }
            }

            valueParameters = (unboundArgs + create1CompletionParameter).mapIndexed { index, parameter ->
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

        private fun buildInvokeMethod(
            functionInvokeFunction: IrSimpleFunction,
            createFunction: IrFunction,
            stateMachineFunction: IrFunction
        ): IrSimpleFunction = context.irFactory.buildFun {
            startOffset = irFunction.startOffset
            endOffset = irFunction.endOffset
            origin = DECLARATION_ORIGIN_COROUTINE_IMPL
            name = OperatorNameConventions.INVOKE
            visibility = DescriptorVisibilities.PROTECTED
            returnType = context.irBuiltIns.anyNType
            isSuspend = true
        }.apply {
            parent = coroutineClass
            coroutineClass.declarations += this

            typeParameters = irFunction.typeParameters.map { parameter ->
                parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                    .apply { superTypes += parameter.superTypes }
            }

            valueParameters = createFunction.valueParameters
                // Skip completion - invoke() already has it implicitly as a suspend function.
                .take(createFunction.valueParameters.size - 1)
                .mapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

            this.createDispatchReceiverParameter()

            overriddenSymbols += functionInvokeFunction.symbol

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

        private fun buildInvokeSuspendMethod(
            stateMachineFunction: IrSimpleFunction,
            coroutineClass: IrClass
        ): IrSimpleFunction {
            val function = context.irFactory.buildFun {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = stateMachineFunction.name
                visibility = stateMachineFunction.visibility
                returnType = context.irBuiltIns.anyNType
                isInline = stateMachineFunction.isInline
                isExternal = stateMachineFunction.isExternal
                isTailrec = stateMachineFunction.isTailrec
                isSuspend = stateMachineFunction.isSuspend
                isOperator = false
                isExpect = stateMachineFunction.isExpect
                isFakeOverride = false
            }.apply {
                parent = coroutineClass
                coroutineClass.declarations += this

                typeParameters = stateMachineFunction.typeParameters.map { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes += parameter.superTypes }
                }

                valueParameters = stateMachineFunction.valueParameters.mapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

                this.createDispatchReceiverParameter()

                overriddenSymbols += stateMachineFunction.symbol
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
        val klass = this
        return factory.buildField {
            this.startOffset = klass.startOffset
            this.endOffset = klass.endOffset
            this.origin = DECLARATION_ORIGIN_COROUTINE_IMPL
            this.name = name
            this.type = type
            this.visibility = DescriptorVisibilities.PRIVATE
            this.isFinal = !isMutable
        }.also {
            it.parent = this
            addChild(it)
        }
    }

    companion object {
        private val CREATE_IDENTIFIER = Name.identifier("create")
    }
}

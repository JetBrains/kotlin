/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapIndexed
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : BodyLoweringPass {

    private var IrFunction.coroutineConstructor by context.mapping.suspendFunctionToCoroutineConstructor

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

    @Suppress("UNUSED_PARAMETER")
    protected fun initializeStateMachine(coroutineConstructors: List<IrConstructor>, coroutineClassThis: IrValueDeclaration) {
        // Do nothing by default
        // TODO find out if Kotlin/Native needs this method.
    }

    protected open fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression =
        delegatingCall

    private val builtCoroutines = hashMapOf<IrFunction, BuiltCoroutine>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction && container.isSuspend) {
            transformSuspendFunction(container, irBody)?.let {
                val dc = container.parent as IrDeclarationContainer
                dc.addChild(it)
            }
        }
    }

    private fun getSuspendFunctionKind(function: IrSimpleFunction, body: IrBody): SuspendFunctionKind {

        fun IrSimpleFunction.isSuspendLambda() =
            name.asString() == "invoke" && parentClassOrNull?.let { it.origin === CallableReferenceLowering.Companion.LAMBDA_IMPL } == true

        if (function.isSuspendLambda())
            return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

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
        // This happens a lot in practice because of suspend functions with default arguments.
        // TODO: use TailRecursionCallsCollector.
        val lastCall = when (val lastStatement = (body as IrBlockBody).statements.lastOrNull()) {
            is IrCall ->
                // Delegation to call without return can only be performed to Unit-returning function call from Unit-returning function
                if (lastStatement.type == context.irBuiltIns.unitType && function.returnType == context.irBuiltIns.unitType)
                    lastStatement
                else
                    null
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

    private fun transformSuspendFunction(function: IrSimpleFunction, body: IrBody): IrClass? {
        assert(function.isSuspend)

        return when (val functionKind = getSuspendFunctionKind(function, body)) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(function, functionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val coroutine = buildCoroutine(function)      // Coroutine implementation.
                if (coroutine === function.parent)             // Suspend lambdas are called through factory method <create>,
                    null
                else
                    coroutine
            }
        }
    }

    private fun IrBlockBodyBuilder.createCoroutineInstance(function: IrSimpleFunction, parameters: Collection<IrValueParameter>, coroutine: BuiltCoroutine): IrExpression {
        val constructor = coroutine.coroutineConstructor
        val coroutineTypeArgs = function.typeParameters.memoryOptimizedMap {
            IrSimpleTypeImpl(it.symbol, true, emptyList(), emptyList())
        }

        return irCallConstructor(constructor.symbol, coroutineTypeArgs).apply {
            parameters.forEachIndexed { index, argument ->
                putValueArgument(index, irGet(argument))
            }
            putValueArgument(
                parameters.size,
                irCall(
                    getContinuationSymbol,
                    getContinuationSymbol.owner.returnType,
                    listOf(function.returnType)
                )
            )
        }
    }

    private fun buildCoroutine(function: IrSimpleFunction): IrClass {
        val coroutine = CoroutineBuilder(function).build()

        val isSuspendLambda = coroutine.coroutineClass === function.parent

        if (isSuspendLambda) return coroutine.coroutineClass

        // It is not a lambda - replace original function with a call to constructor of the built coroutine.

        with(function) {
            val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val functionBody = body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody {
                generateCoroutineStart(coroutine.stateMachineFunction, createCoroutineInstance(this@with, explicitParameters, coroutine))
            }.statements)
        }

        return coroutine.coroutineClass
    }

    private inner class CoroutineBuilder(private val function: IrSimpleFunction) {
        private val startOffset = function.startOffset
        private val endOffset = function.endOffset
        private val isSuspendLambda = function.isOperator && function.name.asString() == "invoke" && function.parentClassOrNull
            ?.let { it.origin === CallableReferenceLowering.Companion.LAMBDA_IMPL } == true
        private val functionParameters = if (isSuspendLambda) function.valueParameters else function.explicitParameters

        private val coroutineClass: IrClass = getCoroutineClass(function)

        private val coroutineClassThis = coroutineClass.thisReceiver!!

        private val continuationType = continuationClassSymbol.typeWith(function.returnType)

        // Save all arguments to fields.
        private val argumentToPropertiesMap = functionParameters
            .associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(function)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = coroutineBaseClass.owner.simpleFunctions()
            .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private fun getCoroutineClass(function: IrSimpleFunction): IrClass {
            return if (isSuspendLambda) function.parentAsClass
            else buildNewCoroutineClass(function)
        }

        private fun buildNewCoroutineClass(function: IrSimpleFunction): IrClass =
            context.irFactory.buildClass {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(function)
                visibility = function.visibility
            }.apply {
                parent = function.parent
                createParameterDeclarations()
                typeParameters = function.typeParameters.memoryOptimizedMap { typeParam ->
                    // TODO: remap types
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes = superTypes memoryOptimizedPlus typeParam.superTypes }
                }
            }

        private fun buildConstructor(): IrConstructor {
            if (isSuspendLambda) {
                return coroutineClass.declarations
                    .filterIsInstance<IrConstructor>()
                    .single()
                    .let {
                        context.mapping.capturedConstructors[it] ?: it
                    }
            }

            return context.irFactory.buildConstructor {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = coroutineBaseClassConstructor.name
                visibility = function.visibility
                returnType = coroutineClass.defaultType
                isPrimary = true
            }.apply {
                parent = coroutineClass
                coroutineClass.addChild(this)

                valueParameters = functionParameters.memoryOptimizedMapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index, defaultValue = null)
                }
                val continuationParameter = coroutineBaseClassConstructor.valueParameters[0]
                valueParameters = valueParameters memoryOptimizedPlus continuationParameter.copyTo(
                    this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                    index = valueParameters.size,
                    startOffset = function.startOffset,
                    endOffset = function.endOffset,
                    type = continuationType,
                    defaultValue = null
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

        private fun buildInvokeSuspendMethod(stateMachineFunction: IrSimpleFunction): IrSimpleFunction {
            val smFunction = context.irFactory.buildFun {
                startOffset = function.startOffset
                endOffset = function.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = stateMachineFunction.name
                visibility = stateMachineFunction.visibility
                modality = Modality.FINAL
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
                coroutineClass.addChild(this)

                typeParameters = stateMachineFunction.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                valueParameters = stateMachineFunction.valueParameters.memoryOptimizedMapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

                this.createDispatchReceiverParameter()

                overriddenSymbols = listOf(stateMachineFunction.symbol)
            }

            buildStateMachine(smFunction, function, argumentToPropertiesMap)
            return smFunction
        }

        // val i = $lambdaN(this.f1, this.f2, ..., this.fn, continuation) // bound
        // i.s1 = p1 // unbound
        // ...
        // i.sn = pn
        // return i
        private fun buildCreateMethod(superCreateFunction: IrSimpleFunction?, constructor: IrConstructor): IrSimpleFunction =
            context.irFactory.buildFun {
                startOffset = UNDEFINED_OFFSET
                endOffset = UNDEFINED_OFFSET
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = Name.identifier("create")
                visibility = DescriptorVisibilities.PROTECTED
                returnType = coroutineClass.defaultType
            }.apply {
                parent = coroutineClass
                coroutineClass.addChild(this)

                typeParameters = function.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                val unboundArgs = function.valueParameters

                val createValueParameters = (unboundArgs + create1CompletionParameter).memoryOptimizedMapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                }

                valueParameters = createValueParameters

                this.createDispatchReceiverParameter()

                superCreateFunction?.let {
                    overriddenSymbols = ArrayList<IrSimpleFunctionSymbol>(it.overriddenSymbols.size + 1).apply {
                        addAll(it.overriddenSymbols)
                        add(it.symbol)
                    }
                }

                val thisReceiver = this.dispatchReceiverParameter!!

                val boundFields =
                    context.mapping.capturedFields[coroutineClass]
                        ?: compilationException(
                            "No captured values",
                            coroutineClass
                        )

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    val instanceCreate = irCall(constructor).apply {
                        var unboundIndex = 0
                        for (f in boundFields) {
                            putValueArgument(unboundIndex++, irGetField(irGet(thisReceiver), f))
                        }
                        putValueArgument(unboundIndex++, irGet(createValueParameters.last()))
                        assert(unboundIndex == constructor.valueParameters.size) {
                            "Not all arguments of <create> are used"
                        }
                    }
                    val instanceVal = scope.createTmpVariable(instanceCreate, "i")
                    +instanceVal

                    assert(createValueParameters.size - 1 == argumentToPropertiesMap.size)

                    for ((p, f) in createValueParameters.zip(argumentToPropertiesMap.values)) {
                        +irSetField(irGet(instanceVal), f, irGet(p))
                    }

                    +irReturn(irGet(instanceVal))
                }
            }

        private fun transformInvokeMethod(createFunction: IrSimpleFunction, stateMachineFunction: IrSimpleFunction) {
            val irBuilder = context.createIrBuilder(function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val thisReceiver = function.dispatchReceiverParameter
                ?: compilationException(
                    "Expected dispatch receiver for invoke",
                    function
                )
            val functionBody = function.body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                generateCoroutineStart(stateMachineFunction, irCall(createFunction).apply {
                    dispatchReceiver = irGet(thisReceiver)
                    var index = 0
                    for (parameter in function.valueParameters) {
                        putValueArgument(index++, irGet(parameter))
                    }
                    putValueArgument(
                        index++,
                        irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(function.returnType))
                    )
                    assert(index == createFunction.valueParameters.size)
                })
            }.statements)
        }

        fun build(): BuiltCoroutine {
            val coroutineConstructor = buildConstructor()

            val implementedMembers = ArrayList<IrSimpleFunction>(2)

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction)

            implementedMembers.add(invokeSuspendMethod)

            if (isSuspendLambda) {
                // Suspend lambda - create factory methods.
                val createFunction = coroutineBaseClass.owner.simpleFunctions()
                    .atMostOne {
                        it.name.asString() == "create" && it.valueParameters.size == function.valueParameters.size + 1
                    }

                val createMethod = buildCreateMethod(createFunction, coroutineConstructor)
                implementedMembers.add(createMethod)

                transformInvokeMethod(createMethod, invokeSuspendMethod)
            } else {
                coroutineClass.superTypes = coroutineClass.superTypes memoryOptimizedPlus coroutineBaseClass.defaultType
            }

            coroutineClass.addFakeOverrides(context.typeSystem, implementedMembers)

            // TODO constructing fake overrides on lowered declaration is tricky.
            coroutineClass.declarations.transformFlat {
                if (it is IrProperty && it.isFakeOverride) {
                    listOfNotNull(it.getter, it.setter)
                } else null
            }

            // TODO: find out whether Kotlin/Native needs this call
            initializeStateMachine(listOf(coroutineConstructor), coroutineClassThis)

            return BuiltCoroutine(
                coroutineClass = coroutineClass,
                coroutineConstructor = coroutineConstructor,
                stateMachineFunction = invokeSuspendMethod
            )

        }
    }

    // Suppress since it is used in native
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun IrCall.isReturnIfSuspendedCall() =
        symbol.owner.run { fqNameWhenAvailable == context.internalPackageFqn.child(Name.identifier("returnIfSuspended")) }

    private sealed class SuspendFunctionKind {
        object NO_SUSPEND_CALLS : SuspendFunctionKind()
        class DELEGATING(val delegatingCall: IrCall) : SuspendFunctionKind()
        object NEEDS_STATE_MACHINE : SuspendFunctionKind()
    }

    private val symbols = context.ir.symbols
    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
            if (delegatingCall.isReturnIfSuspendedCall())
                delegatingCall.getValueArgument(0)!!
            else delegatingCall
        val body = irFunction.body as IrBlockBody

        // Set both offsets to body.endOffset.previousOffset (check the description of the `previousOffset` method)
        // so that a breakpoint set at the closing brace of a lambda expression could be hit.
        context.createIrBuilder(
            irFunction.symbol,
            startOffset = body.endOffset.previousOffset,
            endOffset = body.endOffset.previousOffset
        ).run {
            val statements = body.statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }

            // Instead of returning right away, we save the value to a temporary variable and after that return that variable.
            // This is done solely to improve the debugging experience. Otherwise, a breakpoint set to the closing brace of the function
            // cannot be hit.
            val tempVar = scope.createTemporaryVariable(
                generateDelegatedCall(irFunction.returnType, returnValue),
                irType = context.irBuiltIns.anyType,
            )
            statements[statements.lastIndex] = tempVar
            statements.add(irReturn(irGet(tempVar)))
        }
    }

    private class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val stateMachineFunction: IrFunction
    )

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
}

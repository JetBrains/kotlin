/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
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

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : BodyLoweringPass {

    private var IrFunction.coroutineConstructor by context.mapping.suspendFunctionToCoroutineConstructor

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
        val coroutineTypeArgs = function.typeParameters.map {
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
            val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
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
        private val argumentToPropertiesMap = functionParameters.associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(function)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = coroutineBaseClass.owner.simpleFunctions()
            .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private fun getCoroutineClass(function: IrSimpleFunction): IrClass {
            return if (isSuspendLambda) function.parentAsClass
            else buildNewCoroutineClass(function)
        }

        private fun buildNewCoroutineClass(function: IrSimpleFunction): IrClass = WrappedClassDescriptor().let { d ->
            IrClassImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrClassSymbolImpl(d),
                nameForCoroutineClass(function),
                ClassKind.CLASS,
                function.visibility,
                Modality.FINAL
            ).apply {
                d.bind(this)
                parent = function.parent
                createParameterDeclarations()
                typeParameters = function.typeParameters.map { typeParam ->
                    // TODO: remap types
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes += typeParam.superTypes }
                }
            }
        }

        private fun buildConstructor(): IrConstructor {
            if (isSuspendLambda) {
                return coroutineClass.declarations.filterIsInstance<IrConstructor>().single().let {
                    context.mapping.capturedConstructors[it] ?: it
                }
            }

            return WrappedClassConstructorDescriptor().let { d ->
                IrConstructorImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    IrConstructorSymbolImpl(d),
                    coroutineBaseClassConstructor.name,
                    function.visibility,
                    coroutineClass.defaultType,
                    isInline = false,
                    isExternal = false,
                    isPrimary = true,
                    isExpect = false
                ).apply {
                    d.bind(this)
                    parent = coroutineClass
                    coroutineClass.addChild(this)

                    valueParameters = functionParameters.mapIndexed { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index, defaultValue = null)
                    }
                    val continuationParameter = coroutineBaseClassConstructor.valueParameters[0]
                    valueParameters += continuationParameter.copyTo(
                        this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                        index = valueParameters.size,
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
        }

        private fun buildInvokeSuspendMethod(stateMachineFunction: IrSimpleFunction): IrSimpleFunction {
            val smFunction = WrappedSimpleFunctionDescriptor().let { d ->
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
                    coroutineClass.addChild(this)

                    typeParameters = stateMachineFunction.typeParameters.map { parameter ->
                        parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                            .apply { superTypes += parameter.superTypes }
                    }

                    valueParameters = stateMachineFunction.valueParameters.mapIndexed { index, parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, index)
                    }

                    this.createDispatchReceiverParameter()

                    overriddenSymbols = listOf(stateMachineFunction.symbol)
                }
            }

            buildStateMachine(smFunction, function, argumentToPropertiesMap)
            return smFunction
        }

        private fun buildCreateMethod(superCreateFunction: IrSimpleFunction?, constructor: IrConstructor): IrSimpleFunction {
            // val i = $lambdaN(this.f1, this.f2, ..., this.fn, continuation) // bound
            // i.s1 = p1 // unbound
            // ...
            // i.sn = pn
            // return i
            val createFunction = WrappedSimpleFunctionDescriptor().let { d ->
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
                    coroutineClass.addChild(this)

                    typeParameters = function.typeParameters.map { parameter ->
                        parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                            .apply { superTypes += parameter.superTypes }
                    }

                    val unboundArgs = function.valueParameters

                    val createValueParameters = (unboundArgs + create1CompletionParameter).mapIndexed { index, parameter ->
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
                        context.mapping.capturedFields[coroutineClass] ?: error("No captured values for class ${coroutineClass.render()}")

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
            }

            return createFunction
        }

        private fun transformInvokeMethod(createFunction: IrSimpleFunction, stateMachineFunction: IrSimpleFunction) {
            val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
            val thisReceiver = function.dispatchReceiverParameter ?: error("Expected dispatch receiver for invoke")
            val functionBody = function.body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody(startOffset, endOffset) {
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
                coroutineClass.superTypes += coroutineBaseClass.defaultType
            }

            coroutineClass.addFakeOverrides(implementedMembers)

            // TODO: to meet PIR lower model constructor modification shouldn't be performed here
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
        context.createIrBuilder(irFunction.symbol).run {
            val statements = (irFunction.body as IrBlockBody).statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }
            statements[statements.lastIndex] = irReturn(generateDelegatedCall(irFunction.returnType, returnValue))
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
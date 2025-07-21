/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.backend.common.lower.ReturnableBlockTransformer
import org.jetbrains.kotlin.backend.common.lower.coroutines.loweredSuspendFunctionReturnType
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.backend.common.lower.WebCallableReferenceLowering
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

/**
 * Transforms suspend function into a `CoroutineImpl` instance and builds a state machine.
 */
class JsSuspendFunctionsLowering(
    ctx: JsCommonBackendContext
) : AbstractSuspendFunctionsLowering<JsCommonBackendContext>(ctx), BodyLoweringPass {
    private val coroutineSymbols = ctx.symbols.coroutineSymbols

    private val coroutineImplExceptionPropertyGetter = coroutineSymbols.coroutineImplExceptionPropertyGetter
    private val coroutineImplExceptionPropertySetter = coroutineSymbols.coroutineImplExceptionPropertySetter
    private val coroutineImplExceptionStatePropertyGetter = coroutineSymbols.coroutineImplExceptionStatePropertyGetter
    private val coroutineImplExceptionStatePropertySetter = coroutineSymbols.coroutineImplExceptionStatePropertySetter
    private val coroutineImplLabelPropertySetter = coroutineSymbols.coroutineImplLabelPropertySetter
    private val coroutineImplLabelPropertyGetter = coroutineSymbols.coroutineImplLabelPropertyGetter
    private val coroutineImplResultSymbolGetter = coroutineSymbols.coroutineImplResultSymbolGetter
    private val coroutineImplResultSymbolSetter = coroutineSymbols.coroutineImplResultSymbolSetter

    override val stateMachineMethodName = Name.identifier("doResume")
    override val coroutineClassAnnotations: List<IrConstructorCall> by lazy(LazyThreadSafetyMode.NONE) {
        ctx.jsExportIgnoreSymbol?.let { listOf(JsIrBuilder.buildConstructorCall(it.constructors.single())) } ?: emptyList()
    }

    override fun getCoroutineBaseClass(function: IrFunction) = context.symbols.coroutineImpl

    override fun nameForCoroutineClass(function: IrFunction) = "${function.name}COROUTINE\$".synthesizedName

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction && container.isSuspend) {
            transformSuspendFunction(container, irBody)?.let {
                val dc = container.parent as IrDeclarationContainer
                dc.addChild(it)
            }
        }
    }

    private fun transformSuspendFunction(function: IrSimpleFunction, body: IrBody): IrClass? {
        assert(function.isSuspend)

        return when (val functionKind = getSuspendFunctionKind(context, function, body)) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(function, functionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val isLoweredSuspendLambda = function.isOperator &&
                        function.name == OperatorNameConventions.INVOKE &&
                        function.parentClassOrNull?.let { it.origin === WebCallableReferenceLowering.LAMBDA_IMPL } == true
                val coroutine = buildCoroutine(function, isLoweredSuspendLambda)      // Coroutine implementation.
                if (isLoweredSuspendLambda)             // Suspend lambdas are called through factory method <create>,
                    null
                else
                    coroutine
            }
        }
    }

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
            if (delegatingCall.isReturnIfSuspendedCall(context))
                delegatingCall.arguments[0]!!
            else delegatingCall

        val body = irFunction.body as IrBlockBody
        val statements = body.statements
        val lastStatement = statements.last()

        context.createIrBuilder(
            irFunction.symbol,
            startOffset = lastStatement.startOffset,
            endOffset = lastStatement.endOffset
        ).run {
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

    override fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>
    ) {
        val returnableBlockTransformer = ReturnableBlockTransformer(context)
        val finallyBlockTransformer = FinallyBlocksLowering(context, context.catchAllThrowableType)
        val simplifiedFunction =
            transformingFunction.transform(finallyBlockTransformer, null).transform(returnableBlockTransformer, null) as IrFunction

        val originalBody = simplifiedFunction.body as IrBlockBody

        val body = IrBlockImpl(
            simplifiedFunction.startOffset,
            simplifiedFunction.endOffset,
            context.irBuiltIns.unitType,
            JsStatementOrigins.STATEMENT_ORIGIN_COROUTINE_IMPL,
            originalBody.statements
        )

        val coroutineClass = stateMachineFunction.parent as IrClass
        val suspendResult = JsIrBuilder.buildVar(
            context.irBuiltIns.anyNType,
            stateMachineFunction,
            "suspendResult",
            true,
            initializer = JsIrBuilder.buildCall(coroutineImplResultSymbolGetter.symbol).apply {
                dispatchReceiver = JsIrBuilder.buildGetValue(stateMachineFunction.dispatchReceiverParameter!!.symbol)
            }
        )

        val suspendState = JsIrBuilder.buildVar(coroutineImplLabelPropertyGetter.returnType, stateMachineFunction, "suspendState", true)

        val unit = context.irBuiltIns.unitType

        val switch = IrWhenImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, unit, JsStatementOrigins.COROUTINE_SWITCH)
        val stateVar = JsIrBuilder.buildVar(context.irBuiltIns.intType, stateMachineFunction)
        val switchBlock = IrBlockImpl(switch.startOffset, switch.endOffset, switch.type).apply {
            statements += stateVar
            statements += switch
        }
        val rootTry = IrTryImpl(body.startOffset, body.endOffset, unit).apply { tryResult = switchBlock }
        val rootLoop = IrDoWhileLoopImpl(
            body.startOffset,
            body.endOffset,
            unit,
            JsStatementOrigins.COROUTINE_ROOT_LOOP,
        ).also {
            it.condition = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
            it.body = rootTry
            it.label = "\$sm"
        }

        val suspendableNodes = collectSuspendableNodes(body)
        val thisReceiver = (stateMachineFunction.dispatchReceiverParameter as IrValueParameter).symbol
        stateVar.initializer = JsIrBuilder.buildCall(coroutineImplLabelPropertyGetter.symbol).apply {
            dispatchReceiver = JsIrBuilder.buildGetValue(thisReceiver)
        }

        val stateMachineBuilder = StateMachineBuilder(
            suspendableNodes,
            context,
            stateMachineFunction.symbol,
            rootLoop,
            coroutineImplExceptionPropertyGetter,
            coroutineImplExceptionPropertySetter,
            coroutineImplExceptionStatePropertyGetter,
            coroutineImplExceptionStatePropertySetter,
            coroutineImplLabelPropertySetter,
            thisReceiver,
            getSuspendResultAsType = { type ->
                JsIrBuilder.buildImplicitCast(
                    JsIrBuilder.buildGetValue(suspendResult.symbol),
                    type
                )
            },
            setSuspendResultValue = { value ->
                JsIrBuilder.buildSetVariable(
                    suspendResult.symbol,
                    JsIrBuilder.buildImplicitCast(
                        value,
                        context.irBuiltIns.anyNType
                    ),
                    unit
                )
            }
        )

        body.acceptVoid(stateMachineBuilder)

        stateMachineBuilder.finalizeStateMachine()

        rootTry.catches += stateMachineBuilder.globalCatch

        assignStateIds(stateMachineBuilder.entryState, stateVar.symbol, switch, rootLoop)

        // Set exceptionState to the global catch block
        stateMachineBuilder.entryState.entryBlock.run {
            val receiver = JsIrBuilder.buildGetValue(coroutineClass.thisReceiver!!.symbol)
            val exceptionTrapId = stateMachineBuilder.rootExceptionTrap.id
            check(exceptionTrapId >= 0)
            val id = JsIrBuilder.buildInt(context.irBuiltIns.intType, exceptionTrapId)
            statements.add(0, JsIrBuilder.buildCall(coroutineImplExceptionStatePropertySetter.symbol).also { call ->
                call.arguments[0] = receiver
                call.arguments[1] = id
            })
        }

        val functionBody = context.irFactory.createBlockBody(
            stateMachineFunction.startOffset,
            stateMachineFunction.endOffset,
            stateMachineBuilder.allTheIntermediateLocals + suspendResult + rootLoop
        )

        stateMachineFunction.body = functionBody

        // Move return targets to new function
        functionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid(this)

                return if (expression.returnTargetSymbol != simplifiedFunction.symbol)
                    expression
                else
                    JsIrBuilder.buildReturn(stateMachineFunction.symbol, expression.value, expression.type)
            }
        })

        val liveLocals = LivenessAnalysis.run(functionBody, { it is IrCall && it.isSuspend })
            .values.flatten().toSet()

        val localToPropertyMap = hashMapOf<IrValueSymbol, IrFieldSymbol>()
        var localCounter = 0
        // TODO: optimize by using the same property for different locals.
        liveLocals.forEach {
            if (it !== suspendState && it !== suspendResult && it !== stateVar) {
                localToPropertyMap.getOrPut(it.symbol) {
                    coroutineClass.addField(Name.identifier("${it.name}${localCounter++}"), it.type, it.isVar)
                        .symbol
                }
            }
        }
        val isSuspendLambda = transformingFunction.parent === coroutineClass
        val parameters = if (isSuspendLambda) simplifiedFunction.nonDispatchParameters else simplifiedFunction.parameters
        for (parameter in parameters) {
            localToPropertyMap.getOrPut(parameter.symbol) {
                argumentToPropertiesMap.getValue(parameter).symbol
            }
        }

        // TODO find out why some parents are incorrect
        stateMachineFunction.body!!.patchDeclarationParents(stateMachineFunction)
        stateMachineFunction.transform(LiveLocalsTransformer(localToPropertyMap, { JsIrBuilder.buildGetValue(thisReceiver) }, unit), null)
    }

    private fun assignStateIds(entryState: SuspendState, subject: IrVariableSymbol, switch: IrWhen, rootLoop: IrLoop) {
        val visited = mutableSetOf<SuspendState>()

        val sortedStates = DFS.topologicalOrder(listOf(entryState), { it.successors }, { visited.add(it) })
        sortedStates.withIndex().forEach { it.value.id = it.index }

        val eqeqeqInt = context.irBuiltIns.eqeqeqSymbol

        for (state in sortedStates) {
            val condition = JsIrBuilder.buildCall(eqeqeqInt).apply {
                arguments[0] = JsIrBuilder.buildGetValue(subject)
                arguments[1] = JsIrBuilder.buildInt(context.irBuiltIns.intType, state.id)
            }

            switch.branches += IrBranchImpl(state.entryBlock.startOffset, state.entryBlock.endOffset, condition, state.entryBlock)
        }

        val dispatchPointTransformer = DispatchPointTransformer {
            assert(it.id >= 0)
            JsIrBuilder.buildInt(context.irBuiltIns.intType, it.id)
        }

        rootLoop.transformChildrenVoid(dispatchPointTransformer)
    }

    private fun needUnboxingOrUnit(fromType: IrType, toType: IrType): Boolean {
        val icUtils = context.inlineClassesUtils

        return (icUtils.getInlinedClass(fromType) == null && icUtils.getInlinedClass(toType) != null) ||
                (fromType.isUnit() && !toType.isUnit())
    }

    override fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression {
        val functionReturnType = (delegatingCall as? IrCall)?.symbol?.owner?.let { function ->
            loweredSuspendFunctionReturnType(function, context.irBuiltIns)
        } ?: delegatingCall.type

        if (!needUnboxingOrUnit(functionReturnType, expectedType)) return delegatingCall

        return irComposite(resultType = expectedType) {
            val tmp = createTmpVariable(delegatingCall, irType = functionReturnType)
            val coroutineSuspended = irCall(coroutineSymbols.coroutineSuspendedGetter)
            val condition = irEqeqeq(irGet(tmp), coroutineSuspended)
            +irIfThen(context.irBuiltIns.unitType, condition, irReturn(irGet(tmp)))
            +irImplicitCast(irGet(tmp), expectedType)
        }
    }

    override fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression) {
        val dispatchReceiverVar = createTmpVariable(receiver, irType = receiver.type)
        +irCall(coroutineImplResultSymbolSetter).apply {
            arguments[0] = irGet(dispatchReceiverVar)
            arguments[1] = irGetObject(context.irBuiltIns.unitClass)
        }
        +irCall(coroutineImplExceptionPropertySetter).apply {
            arguments[0] = irGet(dispatchReceiverVar)
            arguments[1] = irNull()
        }
        val call = irCall(invokeSuspendFunction.symbol).apply {
            arguments[0] = irGet(dispatchReceiverVar)
        }
        val functionReturnType = scope.scopeOwnerSymbol.assertedCast<IrSimpleFunctionSymbol> { "Expected function symbol" }.owner.returnType
        +irReturn(generateDelegatedCall(functionReturnType, call))
    }
}

internal sealed class SuspendFunctionKind {
    object NO_SUSPEND_CALLS : SuspendFunctionKind()
    class DELEGATING(val delegatingCall: IrCall) : SuspendFunctionKind()
    object NEEDS_STATE_MACHINE : SuspendFunctionKind()
}

internal fun getSuspendFunctionKind(
    context: CommonBackendContext,
    function: IrSimpleFunction,
    body: IrBody,
    includeSuspendLambda: Boolean = true
): SuspendFunctionKind {

    fun IrSimpleFunction.isSuspendLambda() =
        name.asString() == "invoke" && parentClassOrNull?.let { it.origin === WebCallableReferenceLowering.LAMBDA_IMPL } == true

    if (function.isSuspendLambda() && includeSuspendLambda)
        return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

    var numberOfSuspendCalls = 0
    body.acceptVoid(object : IrVisitorVoid() {
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
    // TODO: use collectTailSuspendCalls.
    val lastCall = when (val lastStatement = (body as IrBlockBody).statements.lastOrNull()) {
        is IrCall ->
            // Delegation to call without return can only be performed to Unit-returning function call from Unit-returning function
            if (lastStatement.type == context.irBuiltIns.unitType && function.returnType == context.irBuiltIns.unitType)
                lastStatement
            else
                null
        is IrReturn -> {
            fun IrTypeOperatorCall.isImplicitCast(): Boolean {
                return this.operator == IrTypeOperator.IMPLICIT_CAST || this.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            }

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
                    value is IrTypeOperatorCall && value.isImplicitCast() -> value.argument
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
                && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(lastCall)
        else -> SuspendFunctionKind.NEEDS_STATE_MACHINE
    }
}

// Suppress since it is used in native
@Suppress("MemberVisibilityCanBePrivate")
internal fun IrCall.isReturnIfSuspendedCall(context: JsCommonBackendContext) =
    symbol == context.symbols.returnIfSuspended


/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

class JsSuspendFunctionsLowering(ctx: JsIrBackendContext) : AbstractSuspendFunctionsLowering<JsIrBackendContext>(ctx) {

    private val coroutineImplExceptionPropertyGetter = ctx.coroutineImplExceptionPropertyGetter
    private val coroutineImplExceptionPropertySetter = ctx.coroutineImplExceptionPropertySetter
    private val coroutineImplExceptionStatePropertyGetter = ctx.coroutineImplExceptionStatePropertyGetter
    private val coroutineImplExceptionStatePropertySetter = ctx.coroutineImplExceptionStatePropertySetter
    private val coroutineImplLabelPropertySetter = ctx.coroutineImplLabelPropertySetter
    private val coroutineImplLabelPropertyGetter = ctx.coroutineImplLabelPropertyGetter
    private val coroutineImplResultSymbolGetter = ctx.coroutineImplResultSymbolGetter
    private val coroutineImplResultSymbolSetter = ctx.coroutineImplResultSymbolSetter

    private var exceptionTrapId = -1
    private var coroutineId = 0

    override val stateMachineMethodName = Name.identifier("doResume")
    override fun getCoroutineBaseClass(function: IrFunction) = context.ir.symbols.coroutineImpl

    override fun nameForCoroutineClass(function: IrFunction) = "${function.name}COROUTINE\$${coroutineId++}".synthesizedName

    override fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>
    ) {
        val simplifiedFunction = transformingFunction.transform(FinallyBlocksLowering(context, context.dynamicType), null) as IrFunction
        val originalBody = simplifiedFunction.body as IrBlockBody

        val body = IrBlockImpl(
            simplifiedFunction.startOffset,
            simplifiedFunction.endOffset,
            context.irBuiltIns.unitType,
            STATEMENT_ORIGIN_COROUTINE_IMPL,
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

        val switch = IrWhenImpl(body.startOffset, body.endOffset, unit, COROUTINE_SWITCH)
        val rootTry = IrTryImpl(body.startOffset, body.endOffset, unit).apply { tryResult = switch }
        val rootLoop = IrDoWhileLoopImpl(
            body.startOffset,
            body.endOffset,
            unit,
            COROUTINE_ROOT_LOOP,
            rootTry,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
        ).also {
            it.label = "\$sm"
        }

        val suspendableNodes = collectSuspendableNodes(body)
        val thisReceiver = (stateMachineFunction.dispatchReceiverParameter as IrValueParameter).symbol

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
            suspendResult.symbol
        )

        body.acceptVoid(stateMachineBuilder)

        stateMachineBuilder.finalizeStateMachine()

        rootTry.catches += stateMachineBuilder.globalCatch

        assignStateIds(stateMachineBuilder.entryState, thisReceiver, switch, rootLoop)

        exceptionTrapId = stateMachineBuilder.rootExceptionTrap.id

        val functionBody =
            IrBlockBodyImpl(stateMachineFunction.startOffset, stateMachineFunction.endOffset, listOf(suspendResult, rootLoop))

        stateMachineFunction.body = functionBody
        // TODO: Investigate parent problems
        stateMachineFunction.patchDeclarationParents(stateMachineFunction.parent)

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

        val liveLocals = computeLivenessAtSuspensionPoints(functionBody).values.flatten().toSet()

        val localToPropertyMap = mutableMapOf<IrValueSymbol, IrFieldSymbol>()
        var localCounter = 0
        // TODO: optimize by using the same property for different locals.
        liveLocals.forEach {
            if (it != suspendState && it != suspendResult) {
                localToPropertyMap.getOrPut(it.symbol) {
                    coroutineClass.addField(Name.identifier("${it.name}${localCounter++}"), it.type, (it as? IrVariable)?.isVar ?: false)
                        .symbol
                }
            }
        }
        simplifiedFunction.explicitParameters.forEach {
            localToPropertyMap.getOrPut(it.symbol) {
                argumentToPropertiesMap.getValue(it).symbol
            }
        }

        stateMachineFunction.transform(LiveLocalsTransformer(localToPropertyMap, { JsIrBuilder.buildGetValue(thisReceiver) }, unit), null)
    }

    private fun assignStateIds(entryState: SuspendState, thisReceiver: IrValueParameterSymbol, switch: IrWhen, rootLoop: IrLoop) {
        val visited = mutableSetOf<SuspendState>()

        val sortedStates = DFS.topologicalOrder(listOf(entryState), { it.successors }, { visited.add(it) })
        sortedStates.withIndex().forEach { it.value.id = it.index }

        val eqeqeqInt = context.irBuiltIns.eqeqeqSymbol

        for (state in sortedStates) {
            val condition = JsIrBuilder.buildCall(eqeqeqInt).apply {
                putValueArgument(0, JsIrBuilder.buildCall(coroutineImplLabelPropertyGetter.symbol).also {
                    it.dispatchReceiver = JsIrBuilder.buildGetValue(thisReceiver)
                })
                putValueArgument(1, JsIrBuilder.buildInt(context.irBuiltIns.intType, state.id))
            }

            switch.branches += IrBranchImpl(state.entryBlock.startOffset, state.entryBlock.endOffset, condition, state.entryBlock)
        }

        val dispatchPointTransformer = DispatchPointTransformer {
            assert(it.id >= 0)
            JsIrBuilder.buildInt(context.irBuiltIns.intType, it.id)
        }

        rootLoop.transformChildrenVoid(dispatchPointTransformer)
    }

    private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrCall, List<IrValueDeclaration>> {
        // TODO: data flow analysis.
        // Just save all visible for now.
        val result = mutableMapOf<IrCall, List<IrValueDeclaration>>()
        body.acceptChildrenVoid(object : VariablesScopeTracker() {
            override fun visitCall(expression: IrCall) {
                if (!expression.isSuspend) return super.visitCall(expression)

                expression.acceptChildrenVoid(this)
                val visibleVariables = mutableListOf<IrValueDeclaration>()
                scopeStack.forEach { visibleVariables += it }
                result[expression] = visibleVariables
            }
        })

        return result
    }


    override fun initializeStateMachine(coroutineConstructors: List<IrConstructor>, coroutineClassThis: IrValueDeclaration) {
        for (it in coroutineConstructors) {
            (it.body as? IrBlockBody)?.run {
                val receiver = JsIrBuilder.buildGetValue(coroutineClassThis.symbol)
                assert(exceptionTrapId >= 0)
                val id = JsIrBuilder.buildInt(context.irBuiltIns.intType, exceptionTrapId)
                statements += JsIrBuilder.buildCall(coroutineImplExceptionStatePropertySetter.symbol).also { call ->
                    call.dispatchReceiver = receiver
                    call.putValueArgument(0, id)
                }
            }
        }
    }

    override fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression {
        val fromType = (delegatingCall as? IrCall)?.symbol?.owner?.returnType ?: delegatingCall.type
        if (!needUnboxingOrUnit(fromType, expectedType)) return delegatingCall

        val ctx = this@JsSuspendFunctionsLowering.context
        return irComposite(resultType = fromType) {
            val tmp = createTmpVariable(delegatingCall, irType = fromType)
            val coroutineSuspended = irCall(ctx.coroutineSuspendGetter)
            val condition = irEqeqeq(irGet(tmp), coroutineSuspended)
            +irIfThen(fromType, condition, irReturn(irReinterpretCast(irGet(tmp), expectedType)))
            +irGet(tmp)
        }
    }

    override fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression) {
        val dispatchReceiverVar = createTmpVariable(receiver, irType = receiver.type)
        +irCall(coroutineImplResultSymbolSetter).apply {
            dispatchReceiver = irGet(dispatchReceiverVar)
            putValueArgument(0, irGetObject(context.irBuiltIns.unitClass))
        }
        +irCall(coroutineImplExceptionPropertySetter).apply {
            dispatchReceiver = irGet(dispatchReceiverVar)
            putValueArgument(0, irNull())
        }
        val call = irCall(invokeSuspendFunction.symbol).apply {
            dispatchReceiver = irGet(dispatchReceiverVar)
        }
        val functionReturnType = scope.scopeOwnerSymbol.assertedCast<IrSimpleFunctionSymbol> { "Expected function symbol" }.owner.returnType
        +irReturn(generateDelegatedCall(functionReturnType, call))
    }
}
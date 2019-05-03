/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

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

    override val stateMachineMethodName = Name.identifier("doResume")
    override fun getCoroutineBaseClass(function: IrFunction) = context.ir.symbols.coroutineImpl

    override fun buildStateMachine(
        originalBody: IrBody,
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>
    ) {
        val body =
            (originalBody as IrBlockBody).run {
                IrBlockImpl(
                    transformingFunction.startOffset,
                    transformingFunction.endOffset,
                    context.irBuiltIns.unitType,
                    STATEMENT_ORIGIN_COROUTINE_IMPL,
                    statements
                )
            }

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
        )

        val suspendableNodes = mutableSetOf<IrElement>()
        val loweredBody =
            collectSuspendableNodes(body, suspendableNodes, context, stateMachineFunction, context.dynamicType)
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

        loweredBody.acceptVoid(stateMachineBuilder)

        stateMachineBuilder.finalizeStateMachine()

        rootTry.catches += stateMachineBuilder.globalCatch

        val visited = mutableSetOf<SuspendState>()

        val sortedStates = DFS.topologicalOrder(listOf(stateMachineBuilder.entryState), { it.successors }, { visited.add(it) })
        sortedStates.withIndex().forEach { it.value.id = it.index }

        fun buildDispatch(target: SuspendState) = target.run {
            assert(id >= 0)
            JsIrBuilder.buildInt(context.irBuiltIns.intType, id)
        }

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

        rootLoop.transform(DispatchPointTransformer(::buildDispatch), null)

        exceptionTrapId = stateMachineBuilder.rootExceptionTrap.id

        val functionBody =
            IrBlockBodyImpl(stateMachineFunction.startOffset, stateMachineFunction.endOffset, listOf(suspendResult, rootLoop))

        stateMachineFunction.body = functionBody
        // TODO: Investigate parent problems
        stateMachineFunction.patchDeclarationParents(stateMachineFunction.parent)

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
        transformingFunction.explicitParameters.forEach {
            localToPropertyMap.getOrPut(it.symbol) {
                argumentToPropertiesMap.getValue(it).symbol
            }
        }

        stateMachineFunction.transform(LiveLocalsTransformer(localToPropertyMap, { JsIrBuilder.buildGetValue(thisReceiver) }, unit), null)
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
                val id = JsIrBuilder.buildInt(context.irBuiltIns.intType, exceptionTrapId)
                statements += JsIrBuilder.buildCall(coroutineImplExceptionStatePropertySetter.symbol).also { call ->
                    call.dispatchReceiver = receiver
                    call.putValueArgument(0, id)
                }
            }
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
        +irReturn(irCall(invokeSuspendFunction.symbol).apply {
            dispatchReceiver = irGet(dispatchReceiverVar)
        })
    }
}
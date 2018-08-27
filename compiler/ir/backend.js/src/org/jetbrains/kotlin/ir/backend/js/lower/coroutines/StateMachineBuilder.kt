/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.*

class SuspendState(type: IrType) {
    val entryBlock: IrContainerExpression = JsIrBuilder.buildComposite(type)
    val successors = mutableSetOf<SuspendState>()
    var id = -1
}

data class LoopBounds(val headState: SuspendState, val exitState: SuspendState)

data class FinallyTargets(val normal: SuspendState, val fromThrow: SuspendState)

data class TryState(val tryState: SuspendState, val catchState: SuspendState, val finallyState: FinallyTargets?)

class IrDispatchPoint(val target: SuspendState) : IrExpressionBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.entryBlock.type) {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {}

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {}
}


class DispatchPointTransformer(val action: (SuspendState) -> IrExpression) : IrElementTransformerVoid() {
    override fun visitExpression(expression: IrExpression): IrExpression {
        val dispatchPoint = expression as? IrDispatchPoint
            ?: return super.visitExpression(expression)
        return action(dispatchPoint.target)
    }
}

class StateMachineBuilder(
    private val suspendableNodes: MutableSet<IrElement>,
    val context: JsIrBackendContext,
    val function: IrFunctionSymbol,
    private val rootLoop: IrLoop,
    private val exceptionSymbol: IrFieldSymbol,
    private val exStateSymbol: IrFieldSymbol,
    private val stateSymbol: IrFieldSymbol,
    thisSymbol: IrValueParameterSymbol,
    private val suspendResult: IrVariableSymbol
) : IrElementVisitorVoid {

    private val loopMap = mutableMapOf<IrLoop, LoopBounds>()
    private val unit = context.irBuiltIns.unitType
    private val nothing = context.irBuiltIns.nothingType
    private val int = context.irBuiltIns.intType
    private val booleanNotSymbol = context.irBuiltIns.booleanNotSymbol
    private val eqeqeqSymbol = context.irBuiltIns.eqeqeqSymbol

    private val thisReceiver = JsIrBuilder.buildGetValue(thisSymbol)

    private var hasExceptions = false

    val entryState = SuspendState(unit)
    val rootExceptionTrap = buildExceptionTrapState()
    private val globalExceptionVar = JsIrBuilder.buildVar(exceptionSymbol.owner.type, function.owner, "e")
    lateinit var globalCatch: IrCatch

    fun finalizeStateMachine() {
        val unitValue = JsIrBuilder.buildGetObjectValue(
            unit,
            context.symbolTable.referenceClass(context.builtIns.unit)
        )
        globalCatch = buildGlobalCatch()
        if (currentBlock.statements.lastOrNull() !is IrReturn) {
            addStatement(JsIrBuilder.buildReturn(function, unitValue, nothing))
        }
        if (!hasExceptions) entryState.successors += rootExceptionTrap
    }

    private fun buildGlobalCatch(): IrCatch {

        val catchVariable = globalExceptionVar
        val globalExceptionSymbol = globalExceptionVar.symbol
        val block = JsIrBuilder.buildBlock(unit)
        if (hasExceptions) {
            val thenBlock = JsIrBuilder.buildBlock(unit)
            val elseBlock = JsIrBuilder.buildBlock(unit)
            val check = JsIrBuilder.buildCall(eqeqeqSymbol).apply {
                putValueArgument(0, exceptionState())
                putValueArgument(1, IrDispatchPoint(rootExceptionTrap))
            }
            block.statements += JsIrBuilder.buildIfElse(unit, check, thenBlock, elseBlock)
            thenBlock.statements += JsIrBuilder.buildThrow(
                nothing,
                JsIrBuilder.buildGetValue(globalExceptionSymbol)
            )

            // TODO: exception table
            elseBlock.statements += JsIrBuilder.buildSetField(
                stateSymbol,
                thisReceiver,
                exceptionState(),
                unit
            )
            elseBlock.statements += JsIrBuilder.buildSetField(
                exceptionSymbol,
                thisReceiver,
                JsIrBuilder.buildGetValue(globalExceptionSymbol),
                unit
            )
        } else {
            block.statements += JsIrBuilder.buildThrow(
                nothing,
                JsIrBuilder.buildGetValue(globalExceptionSymbol)
            )
        }

        return JsIrBuilder.buildCatch(catchVariable, block)
    }

    private var currentState = entryState
    private var currentBlock = entryState.entryBlock

    private val returnableBlockMap = mutableMapOf<IrReturnableBlockSymbol, Pair<SuspendState, IrVariableSymbol?>>()

    private val catchBlockStack = mutableListOf(rootExceptionTrap)

    private fun buildExceptionTrapState(): SuspendState {
        val state = SuspendState(unit)
        state.entryBlock.statements += JsIrBuilder.buildThrow(nothing, pendingException())
        return state
    }

    private fun newState() {
        val newState = SuspendState(unit)
        doDispatch(newState)
        updateState(newState)
    }

    private fun updateState(newState: SuspendState) {
        currentState = newState
        currentBlock = newState.entryBlock
    }

    private fun lastExpression() = currentBlock.statements.lastOrNull() as? IrExpression ?: unitValue

    private fun IrContainerExpression.addStatement(statement: IrStatement) {
        statements.add(statement)
    }

    private fun addStatement(statement: IrStatement) = currentBlock.addStatement(statement)

    private fun maybeDoDispatch(target: SuspendState) {
        val lastStatement = currentBlock.statements.lastOrNull()
        if (lastStatement !is IrReturn && lastStatement !is IrContinue && lastStatement !is IrThrow) {
            doDispatch(target)
        }
    }

    private fun doDispatch(target: SuspendState, andContinue: Boolean = true) = doDispatchImpl(target, currentBlock, andContinue)

    private fun doDispatchImpl(target: SuspendState, block: IrContainerExpression, andContinue: Boolean) {
        val irDispatch = IrDispatchPoint(target)
        currentState.successors.add(target)
        block.addStatement(JsIrBuilder.buildSetField(stateSymbol, thisReceiver, irDispatch, unit))
        if (andContinue) doContinue(block)
    }

    private fun doContinue(block: IrContainerExpression = currentBlock) {
        block.addStatement(JsIrBuilder.buildContinue(nothing, rootLoop))
    }

    private fun transformLastExpression(transformer: (IrExpression) -> IrStatement) {
        val expression = lastExpression()
        val newStatement = transformer(expression)
        currentBlock.statements.let { if (it.isNotEmpty()) it[it.lastIndex] = newStatement else it += newStatement }
    }

    private fun buildDispatchBlock(target: SuspendState) = JsIrBuilder.buildComposite(unit)
        .also { doDispatchImpl(target, it, true) }

    override fun visitElement(element: IrElement) {
        if (element in suspendableNodes) {
            element.acceptChildrenVoid(this)
        } else {
            addStatement(element as IrStatement)
        }

    }

    private fun transformLoop(loop: IrLoop, transformer: (IrLoop, SuspendState /*head*/, SuspendState /*exit*/) -> Unit) {

        if (loop !in suspendableNodes) return addStatement(loop)

        newState()

        val loopHeadState = currentState
        val loopExitState = SuspendState(unit)

        loopMap[loop] = LoopBounds(loopHeadState, loopExitState)

        transformer(loop, loopHeadState, loopExitState)

        loopMap.remove(loop)

        updateState(loopExitState)
    }

    override fun visitWhileLoop(loop: IrWhileLoop) = transformLoop(loop) { l, head, exit ->
        l.condition.acceptVoid(this)

        transformLastExpression {
            val exitCond = JsIrBuilder.buildCall(booleanNotSymbol).apply { putValueArgument(0, it) }
            val irBreak = buildDispatchBlock(exit)
            JsIrBuilder.buildIfElse(unit, exitCond, irBreak)
        }

        l.body?.acceptVoid(this)

        doDispatch(head)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) = transformLoop(loop) { l, head, exit ->
        l.body?.acceptVoid(this)

        l.condition.acceptVoid(this)

        transformLastExpression {
            val irContinue = buildDispatchBlock(head)
            JsIrBuilder.buildIfElse(unit, it, irContinue)
        }

        doDispatch(exit)
    }

    private fun processReturnableBlock(expression: IrReturnableBlock) {

        if (expression !in suspendableNodes) return super.visitBlock(expression)

        val exitState = SuspendState(unit)
        val resultVariable = if (hasResultingValue(expression)) {
            val irVar = tempVar(expression.type, "RETURNABLE_BLOCK")
            addStatement(irVar)
            irVar.symbol
        } else null

        returnableBlockMap[expression.symbol] = Pair(exitState, resultVariable)

        super.visitBlock(expression)

        returnableBlockMap.remove(expression.symbol)

        maybeDoDispatch(exitState)

        updateState(exitState)

        if (resultVariable != null) {
            addStatement(JsIrBuilder.buildGetValue(resultVariable))
        }
    }

    override fun visitBlock(expression: IrBlock) =
        if (expression is IrReturnableBlock) processReturnableBlock(expression) else super.visitBlock(expression)

    private fun implicitCast(value: IrExpression, toType: IrType) =
        JsIrBuilder.buildTypeOperator(toType, IrTypeOperator.IMPLICIT_CAST, value, toType, toType.classifierOrFail)

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        if (expression.isSuspend) {
            val result = lastExpression()
            val continueState = SuspendState(unit)
            val dispatch = IrDispatchPoint(continueState)

            currentState.successors += continueState

            transformLastExpression { JsIrBuilder.buildSetField(stateSymbol, thisReceiver, dispatch, unit) }

            addStatement(JsIrBuilder.buildSetVariable(suspendResult, result, unit))

            val irReturn = JsIrBuilder.buildReturn(function, JsIrBuilder.buildGetValue(suspendResult), nothing)
            val check = JsIrBuilder.buildCall(eqeqeqSymbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(suspendResult))
                putValueArgument(1, JsIrBuilder.buildCall(context.ir.symbols.coroutineSuspendedGetter))
            }

            val suspensionBlock = JsIrBuilder.buildBlock(unit, listOf(irReturn))
            addStatement(JsIrBuilder.buildIfElse(unit, check, suspensionBlock))
            doContinue()

            updateState(continueState)
            addStatement(implicitCast(JsIrBuilder.buildGetValue(suspendResult), expression.type))
        }
    }

    override fun visitBreak(jump: IrBreak) {
        val exitState = loopMap[jump.loop]!!.exitState
        doDispatch(exitState)
    }

    override fun visitContinue(jump: IrContinue) {
        val headState = loopMap[jump.loop]!!.headState
        doDispatch(headState)
    }

    private fun wrap(expression: IrExpression, variable: IrVariableSymbol) =
        JsIrBuilder.buildSetVariable(variable, expression, unit)

    override fun visitWhen(expression: IrWhen) {

        if (expression !in suspendableNodes) return addStatement(expression)

        val exitState = SuspendState(expression.type)

        val varSymbol: IrVariableSymbol?
        val branches: List<IrBranch>

        if (hasResultingValue(expression)) {
            val irVar = tempVar(expression.type, "WHEN_RESULT")
            varSymbol = irVar.symbol
            addStatement(irVar)

            branches = expression.branches.map {
                val wrapped = wrap(it.result, varSymbol)
                if (it.result in suspendableNodes) {
                    suspendableNodes += wrapped
                }
                when (it) {
                    is IrElseBranch -> IrElseBranchImpl(it.startOffset, it.endOffset, it.condition, wrapped)
                    else /* IrBranch */ -> IrBranchImpl(it.startOffset, it.endOffset, it.condition, wrapped)
                }
            }
        } else {
            varSymbol = null
            branches = expression.branches
        }

        val rootState = currentState
        val rootBlock = currentBlock

        for (branch in branches) {
            if (branch !is IrElseBranch) {
                branch.condition.acceptVoid(this)
                val branchBlock = JsIrBuilder.buildComposite(branch.result.type)
                val elseBlock = JsIrBuilder.buildComposite(expression.type)

                val dispatchState = currentState
                transformLastExpression {
                    // TODO: make sure elseBlock is added iff it really needs
                    JsIrBuilder.buildIfElse(unit, it, branchBlock, elseBlock)
                }

                currentBlock = branchBlock
                branch.result.acceptVoid(this)

                if (currentBlock.statements.last() !is IrContinue) {
                    if (currentState !== rootState) {
                        doDispatch(exitState)
                    }
                }

                currentState = dispatchState
                currentBlock = elseBlock
            } else {
                branch.result.acceptVoid(this)
                if (currentBlock.statements.last() !is IrContinue) {
                    if (currentState !== rootState) {
                        doDispatch(exitState)
                    }
                }
                break
            }
        }

        currentState = rootState
        currentBlock = rootBlock
        maybeDoDispatch(exitState)

        updateState(exitState)
        if (varSymbol != null) {
            addStatement(JsIrBuilder.buildGetValue(varSymbol))
        }
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { value = it } }
    }

    override fun visitVariable(declaration: IrVariable) {
        if (declaration !in suspendableNodes) return addStatement(declaration)
        declaration.acceptChildrenVoid(this)
        transformLastExpression { declaration.apply { initializer = it } }
    }

    override fun visitGetField(expression: IrGetField) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { receiver = it } }
    }

    override fun visitGetClass(expression: IrGetClass) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { argument = it } }
    }

    private fun transformArguments(arguments: Array<IrExpression?>): Array<IrExpression?> {

        var suspendableCount = arguments.fold(0) { r, n -> if (n in suspendableNodes) r + 1 else r }

        val newArguments = arrayOfNulls<IrExpression>(arguments.size)

        for ((i, arg) in arguments.withIndex()) {
            newArguments[i] = if (arg != null && suspendableCount > 0) {
                if (arg in suspendableNodes) suspendableCount--
                arg.acceptVoid(this)
                val irVar = tempVar(arg.type, "ARGUMENT")
                transformLastExpression {
                    irVar.apply { initializer = it }
                }
                JsIrBuilder.buildGetValue(irVar.symbol)
            } else arg
        }

        return newArguments
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression) {

        if (expression !in suspendableNodes) {
            addExceptionEdge()
            return addStatement(expression)
        }

        val arguments = arrayOfNulls<IrExpression>(expression.valueArgumentsCount + 2)
        arguments[0] = expression.dispatchReceiver
        arguments[1] = expression.extensionReceiver

        for (i in 0 until expression.valueArgumentsCount) {
            arguments[i + 2] = expression.getValueArgument(i)
        }

        val newArguments = transformArguments(arguments)

        expression.dispatchReceiver = newArguments[0]
        expression.extensionReceiver = newArguments[1]
        for (i in 0 until expression.valueArgumentsCount) {
            expression.putValueArgument(i, newArguments[i + 2])
        }

        addExceptionEdge()
        addStatement(expression)
    }

    override fun visitSetField(expression: IrSetField) {
        if (expression !in suspendableNodes) return addStatement(expression)

        val newArguments = transformArguments(arrayOf(expression.receiver, expression.value))

        val receiver = newArguments[0]
        val value = newArguments[1] as IrExpression

        addStatement(expression.run {
            IrSetFieldImpl(
                startOffset,
                endOffset,
                symbol,
                receiver,
                value,
                unit,
                origin,
                superQualifierSymbol
            )
        })
    }

    // TODO: should it be lowered before?
    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        assert(expression in suspendableNodes)

        val arguments = arrayOfNulls<IrExpression>(expression.arguments.size)

        expression.arguments.forEachIndexed { i, a -> arguments[i] = a }

        val newArguments = transformArguments(arguments)

        addStatement(expression.run {
            IrStringConcatenationImpl(
                startOffset,
                endOffset,
                type,
                newArguments.map { it!! })
        })
    }

    private val unitValue = JsIrBuilder.buildGetObjectValue(
        unit,
        context.symbolTable.referenceClass(context.builtIns.unit)
    )

    override fun visitReturn(expression: IrReturn) {
        expression.acceptChildrenVoid(this)
        if (expression.returnTargetSymbol is IrReturnableBlockSymbol) {
            val (exitState, varSymbol) = returnableBlockMap[expression.returnTargetSymbol]!!
            if (varSymbol != null) {
                transformLastExpression { JsIrBuilder.buildSetVariable(varSymbol, it, it.type) }
            }
            doDispatch(exitState)
        } else {
            transformLastExpression { expression.apply { value = it } }
        }
    }

    private fun addExceptionEdge() {
        hasExceptions = true
        currentState.successors += catchBlockStack.peek()!!
    }

    private fun hasResultingValue(expression: IrExpression) = expression.type.run { !isUnit() && !isNothing() }

    override fun visitThrow(expression: IrThrow) {
        expression.acceptChildrenVoid(this)
        addExceptionEdge()
        transformLastExpression { expression.apply { value = it } }
    }

    override fun visitTry(aTry: IrTry) {
        val tryState = buildTryState(aTry)
        val enclosingCatch = catchBlockStack.peek()!!

        catchBlockStack.push(tryState.catchState)

        val finallyStateVar = tempVar(int, "FINALLY_STATE")
        val exitState = SuspendState(unit)

        val varSymbol = if (hasResultingValue(aTry)) tempVar(aTry.type, "TRY_RESULT") else null

        if (aTry.finallyExpression != null) {
            finallyStateVar.initializer = IrDispatchPoint(exitState)
            addStatement(finallyStateVar)
        }
        if (varSymbol != null) {
            addStatement(varSymbol)
        }

        // TODO: refact it with exception table, see coroutinesInternal.kt
        setupExceptionState(tryState.catchState)

        val tryResult = if (varSymbol != null) {
            JsIrBuilder.buildSetVariable(varSymbol.symbol, aTry.tryResult, unit).also {
                if (it.value in suspendableNodes) suspendableNodes += it
            }
        } else aTry.tryResult

        tryResult.acceptVoid(this)

        if (tryState.finallyState != null) {
            doDispatch(tryState.finallyState.normal)
        } else {
            setupExceptionState(enclosingCatch)
            doDispatch(exitState)
        }

        addExceptionEdge()
        catchBlockStack.pop()
        updateState(tryState.catchState)

        if (tryState.finallyState != null) {
            setupExceptionState(tryState.finallyState.fromThrow)
        } else {
            setupExceptionState(enclosingCatch)
        }

        val ex = pendingException()

        var rethrowNeeded = true

        for (catch in aTry.catches) {
            val type = catch.catchParameter.type
            val initializer = if (type !is IrDynamicType) implicitCast(ex, type) else ex
            val irVar = catch.catchParameter.also {
                it.initializer = initializer
            }
            val catchResult = if (varSymbol != null) {
                JsIrBuilder.buildSetVariable(varSymbol.symbol, catch.result, unit).also {
                    if (it.value in suspendableNodes) suspendableNodes += it
                }
            } else catch.result

            if (type is IrDynamicType) {
                rethrowNeeded = false

                addStatement(irVar)
                catchResult.acceptVoid(this)
                val exitDispatch = tryState.finallyState?.run { normal } ?: exitState
                maybeDoDispatch(exitDispatch)

            } else {
                val check = buildIsCheck(ex, type)

                val branchBlock = JsIrBuilder.buildComposite(catchResult.type)

                val elseBlock = JsIrBuilder.buildComposite(catchResult.type)
                val irIf = JsIrBuilder.buildIfElse(catchResult.type, check, branchBlock, elseBlock)
                val ifBlock = currentBlock

                currentBlock = branchBlock

                addStatement(irVar)
                catchResult.acceptVoid(this)
                val exitDispatch = tryState.finallyState?.run { normal } ?: exitState
                maybeDoDispatch(exitDispatch)

                currentBlock = ifBlock
                addStatement(irIf)
                currentBlock = elseBlock
            }
        }

        if (rethrowNeeded) {
            addExceptionEdge()
            addStatement(JsIrBuilder.buildThrow(nothing, ex))
        }

        if (tryState.finallyState == null) {
            currentState.successors += enclosingCatch
        }

        val finallyState = tryState.finallyState
        if (finallyState != null) {
            val throwExitState = SuspendState(unit)
            updateState(finallyState.fromThrow)
            tryState.tryState.successors += finallyState.fromThrow
            addStatement(
                JsIrBuilder.buildSetVariable(
                    finallyStateVar.symbol,
                    IrDispatchPoint(throwExitState), int
                )
            )
            doDispatch(finallyState.normal)

            updateState(finallyState.normal)
            tryState.tryState.successors += finallyState.normal
            setupExceptionState(enclosingCatch)
            aTry.finallyExpression?.acceptVoid(this)
            currentState.successors += listOf(throwExitState, exitState)
            addStatement(
                JsIrBuilder.buildSetField(
                    stateSymbol,
                    thisReceiver,
                    JsIrBuilder.buildGetValue(
                        finallyStateVar.symbol
                    ),
                    unit
                )
            )
            doContinue()

            updateState(throwExitState)
            addStatement(JsIrBuilder.buildThrow(nothing, pendingException()))
            addExceptionEdge()
        }

        updateState(exitState)
        if (varSymbol != null) {
            addStatement(JsIrBuilder.buildGetValue(varSymbol.symbol))
        }
    }

    private fun setupExceptionState(target: SuspendState) {
        addStatement(
            JsIrBuilder.buildSetField(
                exStateSymbol, thisReceiver,
                IrDispatchPoint(target), unit
            )
        )
    }

    private fun exceptionState() = JsIrBuilder.buildGetField(exStateSymbol, thisReceiver)
    private fun pendingException() = JsIrBuilder.buildGetField(exceptionSymbol, thisReceiver)

    private fun buildTryState(aTry: IrTry) =
        TryState(
            currentState,
            SuspendState(unit),
            aTry.finallyExpression?.run {
                FinallyTargets(
                    SuspendState(
                        unit
                    ), SuspendState(unit)
                )
            }
        )


    private fun buildIsCheck(value: IrExpression, toType: IrType) =
        JsIrBuilder.buildTypeOperator(
            context.irBuiltIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            value,
            toType,
            toType.classifierOrNull!!
        )

    private fun tempVar(type: IrType, name: String = "tmp") =
        JsIrBuilder.buildVar(type, function.owner, name)
}
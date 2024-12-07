/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.lower.FINALLY_EXPRESSION
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.previousOffset
import org.jetbrains.kotlin.ir.visitors.*

class SuspendState(type: IrType) {
    val entryBlock: IrContainerExpression = JsIrBuilder.buildComposite(type)
    val successors = mutableSetOf<SuspendState>()
    var id = -1
}

data class LoopBounds(val headState: SuspendState, val exitState: SuspendState)

data class TryState(val tryState: SuspendState, val catchState: SuspendState)

class IrDispatchPoint(val target: SuspendState) : IrExpression() {
    override val startOffset: Int get() = UNDEFINED_OFFSET
    override val endOffset: Int get() = UNDEFINED_OFFSET

    override var type: IrType
        get() = target.entryBlock.type
        set(value) {
            target.entryBlock.type = value
        }

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D) = visitor.visitExpression(this, data)
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
    val context: JsCommonBackendContext,
    val function: IrFunctionSymbol,
    private val rootLoop: IrLoop,
    private val exceptionSymbolGetter: IrSimpleFunction,
    private val exceptionSymbolSetter: IrSimpleFunction,
    private val exStateSymbolGetter: IrSimpleFunction,
    private val exStateSymbolSetter: IrSimpleFunction,
    private val stateSymbolSetter: IrSimpleFunction,
    private val thisSymbol: IrValueParameterSymbol,
    private val getSuspendResultAsType: (IrType) -> IrExpression,
    private val setSuspendResultValue: (IrExpression) -> IrStatement
) : IrElementVisitorVoid {

    private val loopMap = hashMapOf<IrLoop, LoopBounds>()
    private val unit = context.irBuiltIns.unitType
    private val anyN = context.irBuiltIns.anyNType
    private val nothing = context.irBuiltIns.nothingType
    private val booleanNotSymbol = context.irBuiltIns.booleanNotSymbol
    private val eqeqeqSymbol = context.irBuiltIns.eqeqeqSymbol

    private val thisReceiver get() = JsIrBuilder.buildGetValue(thisSymbol)

    private var hasExceptions = false

    val entryState = SuspendState(unit)
    val rootExceptionTrap = buildExceptionTrapState()
    private val globalExceptionVar = JsIrBuilder.buildVar(exceptionSymbolGetter.returnType.makeNotNull(), function.owner, "e")
    lateinit var globalCatch: IrCatch

    fun finalizeStateMachine() {
        globalCatch = buildGlobalCatch()
        if (currentBlock.statements.lastOrNull() !is IrReturn) {
            // Set both offsets to rootLoop.endOffset.previousOffset (check the description of the `previousOffset` method)
            // so that a breakpoint set at the closing brace of a lambda expression could be hit.
            // NOTE: rootLoop's offsets are the same as in the original function.
            addStatement(
                IrReturnImpl(
                    startOffset = rootLoop.endOffset.previousOffset,
                    endOffset = rootLoop.endOffset.previousOffset,
                    nothing,
                    function,
                    unitValue
                )
            )
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
                arguments[0] = exceptionState()
                arguments[1] = IrDispatchPoint(rootExceptionTrap)
            }
            block.statements += JsIrBuilder.buildIfElse(unit, check, thenBlock, elseBlock)
            thenBlock.statements += JsIrBuilder.buildThrow(
                nothing,
                JsIrBuilder.buildGetValue(globalExceptionSymbol)
            )

            // TODO: exception table
            elseBlock.statements += JsIrBuilder.buildCall(stateSymbolSetter.symbol, unit).apply {
                arguments[0] = thisReceiver
                arguments[1] = exceptionState()
            }
            elseBlock.statements += JsIrBuilder.buildCall(exceptionSymbolSetter.symbol, unit).apply {
                arguments[0] = thisReceiver
                arguments[1] = JsIrBuilder.buildGetValue(globalExceptionSymbol)
            }
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

    private val catchBlockStack = mutableListOf(rootExceptionTrap)
    private val tryStateMap = hashMapOf<IrExpression, TryState>()
    private val tryLoopStack = mutableListOf<IrExpression>()

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

    private fun isBlockEnded(): Boolean {
        val lastExpression = currentBlock.statements.lastOrNull() as? IrExpression ?: return false
        return lastExpression.type.isNothing()
    }

    private fun maybeDoDispatch(target: SuspendState) {
        if (!isBlockEnded()) {
            doDispatch(target)
        }
    }

    private fun doDispatch(target: SuspendState, andContinue: Boolean = true) = doDispatchImpl(target, currentBlock, andContinue)

    private fun doDispatchImpl(target: SuspendState, block: IrContainerExpression, andContinue: Boolean) {
        val irDispatch = IrDispatchPoint(target)
        currentState.successors.add(target)
        block.addStatement(JsIrBuilder.buildCall(stateSymbolSetter.symbol, unit).apply {
            arguments[0] = thisReceiver
            arguments[1] = irDispatch
        })
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

        tryLoopStack.push(loop)

        transformer(loop, loopHeadState, loopExitState)

        tryLoopStack.pop().also { assert(it === loop) }

        loopMap.remove(loop)

        updateState(loopExitState)
    }

    override fun visitWhileLoop(loop: IrWhileLoop) = transformLoop(loop) { l, head, exit ->
        l.condition.acceptVoid(this)

        transformLastExpression {
            val exitCond = JsIrBuilder.buildCall(booleanNotSymbol).apply { dispatchReceiver = it }
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

    private fun implicitCast(value: IrExpression, toType: IrType) = JsIrBuilder.buildImplicitCast(value, toType)

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        if (expression.isSuspend) {
            val result = lastExpression()
            val expectedType = expression.symbol.owner.returnType
            val isInlineClassExpected = context.inlineClassesUtils.getInlinedClass(expectedType) != null
            val continueState = SuspendState(unit)
            val unboxState = if (isInlineClassExpected) SuspendState(unit) else null

            val dispatch = IrDispatchPoint(unboxState ?: continueState)

            if (unboxState != null) currentState.successors += unboxState

            currentState.successors += continueState

            transformLastExpression {
                JsIrBuilder.buildCall(stateSymbolSetter.symbol, unit).apply {
                    arguments[0] = thisReceiver
                    arguments[1] = dispatch
                }
            }

            addStatement(setSuspendResultValue(result))

            val irReturn = JsIrBuilder.buildReturn(function, getSuspendResultAsType(anyN), nothing)
            val check = JsIrBuilder.buildCall(eqeqeqSymbol).apply {
                arguments[0] = getSuspendResultAsType(anyN)
                arguments[1] = JsIrBuilder.buildCall(context.ir.symbols.coroutineSuspendedGetter)
            }

            val suspensionBlock = JsIrBuilder.buildBlock(unit, listOf(irReturn))
            addStatement(JsIrBuilder.buildIfElse(unit, check, suspensionBlock))

            if (isInlineClassExpected) {
                addStatement(JsIrBuilder.buildCall(stateSymbolSetter.symbol, unit).apply {
                    arguments[0] = thisReceiver
                    arguments[1] = IrDispatchPoint(continueState)
                })
            }

            doContinue()

            unboxState?.let { buildUnboxingState(it, continueState, expectedType) }

            updateState(continueState)
            addStatement(getSuspendResultAsType(expression.type))
        }
    }

    private fun buildUnboxingState(unboxState: SuspendState, continueState: SuspendState, expectedType: IrType) {
        unboxState.successors += continueState
        updateState(unboxState)
        val result = getSuspendResultAsType(anyN)
        val tmp = JsIrBuilder.buildVar(expectedType, function.owner, name = "unboxed", initializer = result)
        addStatement(tmp)
        addStatement(setSuspendResultValue(JsIrBuilder.buildGetValue(tmp.symbol)))

        doDispatch(continueState)
    }

    override fun visitBreak(jump: IrBreak) {
        val exitState = loopMap[jump.loop]!!.exitState
        resetExceptionStateIfNeeded(jump.loop)
        doDispatch(exitState)
    }

    override fun visitContinue(jump: IrContinue) {
        val headState = loopMap[jump.loop]!!.headState
        resetExceptionStateIfNeeded(jump.loop)
        doDispatch(headState)
    }

    private fun resetExceptionStateIfNeeded(loop: IrLoop) {

        /**
         * First find the nearest try statement following after terminating circle
         * In case we have tryLoopStack like this
         *
         * [try 1] <- current exception state
         * [loop] <- terminating loop
         * [try 2] <- enclosing try-catch
         *
         * our goal to find [try 2]
         *
         * Second set exception state to either found try's catch block or root catch
         */

        var nearestTry: IrExpression? = null
        var found = false
        var needReset = false
        for (e in tryLoopStack.asReversed()) {

            if (e is IrTry) {
                needReset = !found
            }

            if (e === loop) {
                found = true
            }

            if (found) {
                if (e is IrTry) {
                    nearestTry = e
                    break
                }
            }
        }

        if (needReset) {
            val tryState = tryStateMap[nearestTry]?.catchState ?: rootExceptionTrap
            setupExceptionState(tryState)
        }
    }

    private fun wrap(expression: IrExpression, variable: IrVariableSymbol) =
        JsIrBuilder.buildSetVariable(variable, expression, unit)

    override fun visitComposite(expression: IrComposite) {
        if (expression.origin == FINALLY_EXPRESSION) {
            catchBlockStack.peek()?.let(::setupExceptionState)
        }
        super.visitComposite(expression)
    }

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
                when {
                    isElseBranch(it) -> IrElseBranchImpl(it.startOffset, it.endOffset, it.condition, wrapped)
                    else /* IrBranch */ -> IrBranchImpl(it.startOffset, it.endOffset, it.condition, wrapped)
                }
            }
        } else {
            varSymbol = null
            branches = expression.branches
        }

        for (branch in branches) {
            if (!isElseBranch(branch)) {
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

                if (!isBlockEnded()) {
                    doDispatch(exitState)
                }

                currentState = dispatchState
                currentBlock = elseBlock
            } else {
                branch.result.acceptVoid(this)
                if (!isBlockEnded()) {
                    doDispatch(exitState)
                }
                break
            }
        }

        maybeDoDispatch(exitState)
        updateState(exitState)

        if (varSymbol != null) {
            addStatement(JsIrBuilder.buildGetValue(varSymbol))
        }
    }

    override fun visitSetValue(expression: IrSetValue) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { value = it } }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { argument = it } }
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

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { receiver = it } }
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) {
        if (expression !in suspendableNodes) return addStatement(expression)
        transformArguments(expression.arguments)
        addStatement(expression)
    }

    override fun visitGetClass(expression: IrGetClass) {
        if (expression !in suspendableNodes) return addStatement(expression)
        expression.acceptChildrenVoid(this)
        transformLastExpression { expression.apply { argument = it } }
    }

    override fun visitVararg(expression: IrVararg) {
        if (expression !in suspendableNodes) return addStatement(expression)
        val spreadIndices = mutableSetOf<Int>()
        val newArgs = expression.elements
            .mapIndexedTo(mutableListOf()) { index, item ->
                if (item is IrSpreadElement) {
                    spreadIndices.add(index)
                    item.expression
                } else {
                    item as IrExpression
                }
            }
            .apply(this::transformArguments)
            .mapIndexed { index, item ->
                if (index in spreadIndices) {
                    IrSpreadElementImpl(
                        item.startOffset,
                        item.endOffset,
                        item
                    )
                } else {
                    item
                }
            }
        addStatement(
            IrVarargImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.varargElementType,
                newArgs
            )
        )
    }

    private fun <E : IrExpression?> transformArguments(arguments: MutableList<E>) {
        var suspendableCount = arguments.fold(0) { r, n -> if (n != null && n in suspendableNodes) r + 1 else r }
        arguments.replaceAll { arg ->
            if (arg.isPure(false)) arg else {
                require(arg != null)
                if (suspendableCount > 0) {
                    if (arg in suspendableNodes) suspendableCount--
                    arg.acceptVoid(this)
                    val irVar = tempVar(arg.type, "ARGUMENT")
                    transformLastExpression {
                        irVar.apply { initializer = it }
                    }
                    @Suppress("UNCHECKED_CAST")
                    JsIrBuilder.buildGetValue(irVar.symbol) as E
                } else {
                    arg.deepCopyWithSymbols(function.owner)
                }
            }
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {

        if (expression !in suspendableNodes) {
            addExceptionEdge()
            return addStatement(expression)
        }

        transformArguments(expression.arguments)

        addExceptionEdge()
        addStatement(expression)
    }

    override fun visitSetField(expression: IrSetField) {
        if (expression !in suspendableNodes) return addStatement(expression)

        val newArguments = mutableListOf(expression.receiver, expression.value).also(this::transformArguments)

        val receiver = newArguments[0]
        val value = newArguments[1]!!

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
        if (expression !in suspendableNodes) return addStatement(expression)

        val newArguments = expression.arguments.toMutableList().apply(this::transformArguments)

        addStatement(expression.run {
            IrStringConcatenationImpl(
                startOffset,
                endOffset,
                type,
                newArguments
            )
        })
    }

    private val unitValue get() = JsIrBuilder.buildGetObjectValue(unit, context.irBuiltIns.unitClass)

    override fun visitReturn(expression: IrReturn) {
        expression.acceptChildrenVoid(this)
        val returnTarget = expression.returnTargetSymbol
        if (returnTarget !is IrReturnableBlockSymbol) {
            transformLastExpression { expression.apply { value = it } }
        }
    }

    private fun addExceptionEdge() {
        hasExceptions = true
        currentState.successors += catchBlockStack.peek()!!
    }

    private fun hasResultingValue(expression: IrExpression) = !expression.type.run { isNothing() || isUnit() }

    override fun visitThrow(expression: IrThrow) {
        expression.acceptChildrenVoid(this)
        addExceptionEdge()
        transformLastExpression { expression.apply { value = it } }
    }

    override fun visitTry(aTry: IrTry) {

        require(aTry.finallyExpression == null)

        val tryState = buildTryState()
        val enclosingCatch = catchBlockStack.peek()!!

        tryStateMap[aTry] = tryState

        catchBlockStack.push(tryState.catchState)
        tryLoopStack.push(aTry)

        val exitState = SuspendState(unit)

        val varSymbol = if (hasResultingValue(aTry)) tempVar(aTry.type, "TRY_RESULT") else null

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

        if (!isBlockEnded()) {
            setupExceptionState(enclosingCatch)
            doDispatch(exitState)
        }
        addExceptionEdge()

        tryStateMap.remove(aTry)
        tryLoopStack.pop().also { assert(it === aTry) }

        catchBlockStack.pop()

        updateState(tryState.catchState)
        setupExceptionState(enclosingCatch)

        var rethrowNeeded = true

        for (catch in aTry.catches) {
            val type = catch.catchParameter.type
            val initializer = if (type !is IrDynamicType) implicitCast(pendingException(), type) else pendingException()
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
                maybeDoDispatch(exitState)
            } else {
                val check = buildIsCheck(pendingException(), type)

                val branchBlock = JsIrBuilder.buildComposite(catchResult.type)

                val elseBlock = JsIrBuilder.buildComposite(catchResult.type)
                val irIf = JsIrBuilder.buildIfElse(catchResult.type, check, branchBlock, elseBlock)
                val ifBlock = currentBlock

                currentBlock = branchBlock

                addStatement(irVar)
                catchResult.acceptVoid(this)
                maybeDoDispatch(exitState)

                currentBlock = ifBlock
                addStatement(irIf)
                currentBlock = elseBlock
            }
        }

        if (rethrowNeeded) {
            addExceptionEdge()
            addStatement(JsIrBuilder.buildThrow(nothing, pendingException()))
        }

        currentState.successors += enclosingCatch

        updateState(exitState)
        setupExceptionState(enclosingCatch)

        if (varSymbol != null) {
            addStatement(JsIrBuilder.buildGetValue(varSymbol.symbol))
        }
    }

    private fun setupExceptionState(target: SuspendState) {
        addStatement(
            JsIrBuilder.buildCall(exStateSymbolSetter.symbol, unit).apply {
                arguments[0] = thisReceiver
                arguments[1] = IrDispatchPoint(target)
            }
        )
    }

    private fun exceptionState() = JsIrBuilder.buildCall(exStateSymbolGetter.symbol).also { it.dispatchReceiver = thisReceiver }
    private fun pendingException() = JsIrBuilder.buildCall(exceptionSymbolGetter.symbol).also { it.dispatchReceiver = thisReceiver }

    private fun buildTryState() = TryState(currentState, SuspendState(unit))

    private fun buildIsCheck(value: IrExpression, toType: IrType) =
        JsIrBuilder.buildTypeOperator(
            context.irBuiltIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            value,
            toType
        )

    private fun tempVar(type: IrType, name: String = "tmp") =
        JsIrBuilder.buildVar(type, function.owner, name)
}

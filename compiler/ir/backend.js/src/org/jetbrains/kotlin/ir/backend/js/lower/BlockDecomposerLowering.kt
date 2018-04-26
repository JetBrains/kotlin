/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.descriptors.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.descriptors.initialize
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

private typealias VisitData = Nothing?

class BlockDecomposerLowering(val context: JsIrBackendContext) : FunctionLoweringPass {

    private lateinit var function: IrFunction
    private var tmpVarCounter: Int = 0

    private val statementVisitor = StatementVisitor()
    private val expressionVisitor = ExpressionVisitor()

    private val constTrue = JsIrBuilder.buildBoolean(context.builtIns.booleanType, true)
    private val constFalse = JsIrBuilder.buildBoolean(context.builtIns.booleanType, false)
    private val nothingType = context.builtIns.nullableNothingType

    private val unitValue =
        JsIrBuilder.buildGetObjectValue(context.builtIns.unitType, context.symbolTable.referenceClass(context.builtIns.unit))

    private val unreachableFunction =
        JsSymbolBuilder.buildSimpleFunction(context.module, Namer.UNREACHABLE_NAME).initialize(type = nothingType)

    override fun lower(irFunction: IrFunction) {
        function = irFunction
        tmpVarCounter = 0
        irFunction.body?.accept(statementVisitor, null)
    }

    enum class VisitStatus {
        DECOMPOSED,
        TERMINATED,
        KEPT
    }

    abstract class VisitResult(val status: VisitStatus) {
        open val statements: MutableList<IrStatement> get() = error("No statement is possible here")
        open val resultValue: IrExpression get() = error("This result has no value")

        abstract fun runIfChanged(action: VisitResult.() -> VisitResult): VisitResult
        abstract fun <T> runIfChangedOrDefault(default: T, action: VisitResult.(d: T) -> T): T
        abstract fun applyIfChanged(action: VisitResult.() -> Unit): VisitResult

    }

    object KeptResult : VisitResult(VisitStatus.KEPT) {
        override fun runIfChanged(action: VisitResult.() -> VisitResult) = this
        override fun <T> runIfChangedOrDefault(default: T, action: VisitResult.(d: T) -> T) = default
        override fun applyIfChanged(action: VisitResult.() -> Unit) = this
    }

    abstract class ChangedResult(
        override val statements: MutableList<IrStatement>,
        override val resultValue: IrExpression,
        status: VisitStatus
    ) :
        VisitResult(status) {
        override fun runIfChanged(action: VisitResult.() -> VisitResult) = run { action() }
        override fun <T> runIfChangedOrDefault(default: T, action: VisitResult.(d: T) -> T) = run { action(default) }
        override fun applyIfChanged(action: VisitResult.() -> Unit) = apply { action() }
    }

    class DecomposedResult(statements: MutableList<IrStatement>, resultValue: IrExpression) :
        ChangedResult(statements, resultValue, VisitStatus.DECOMPOSED) {

        constructor(statement: IrStatement, resultValue: IrExpression) : this(mutableListOf(statement), resultValue)
    }

    class TerminatedResult(statements: MutableList<IrStatement>, resultValue: IrExpression) :
        ChangedResult(statements, resultValue, VisitStatus.TERMINATED)

    abstract inner class DecomposerVisitor : IrElementVisitor<VisitResult, VisitData> {
        override fun visitElement(element: IrElement, data: VisitData) = KeptResult
    }

    inner class StatementVisitor : DecomposerVisitor() {
        override fun visitBlockBody(body: IrBlockBody, data: VisitData): VisitResult {
            body.statements.transformFlat {
                processStatement(it, data)
            }

            return KeptResult
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: VisitData): VisitResult {
            expression.statements.transformFlat {
                processStatement(it, data)
            }

            return KeptResult
        }

        private fun processStatement(statement: IrStatement, data: VisitData): List<IrStatement>? {
            val result = statement.accept(this, data)

            if (result == KeptResult) return null
            return result.statements
        }

        override fun visitExpression(expression: IrExpression, data: VisitData): VisitResult = expression.accept(expressionVisitor, data)

        override fun visitReturn(expression: IrReturn, data: VisitData): VisitResult {
            val expressionResult = expression.value.accept(expressionVisitor, data)

            return expressionResult.runIfChanged {
                val returnValue = expressionResult.resultValue
                expressionResult.statements += IrReturnImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.returnTargetSymbol,
                    returnValue
                )
                DecomposedResult(expressionResult.statements, unitValue)
            }
        }

        override fun visitBreakContinue(jump: IrBreakContinue, data: VisitData) = KeptResult as VisitResult

        override fun visitThrow(expression: IrThrow, data: VisitData): VisitResult {
            val expressionResult = expression.value.accept(expressionVisitor, data)

            return expressionResult.runIfChanged {
                val returnValue = expressionResult.resultValue
                expressionResult.statements += IrThrowImpl(expression.startOffset, expression.endOffset, expression.type, returnValue)
                DecomposedResult(expressionResult.statements, unitValue)
            }
        }

        override fun visitVariable(declaration: IrVariable, data: VisitData): VisitResult {
            declaration.initializer?.let {
                val initResult = it.accept(expressionVisitor, data)
                return initResult.runIfChanged {
                    statements += declaration.apply { initializer = resultValue }
                    DecomposedResult(statements, unitValue)
                }
            }

            return KeptResult
        }

        override fun visitWhen(expression: IrWhen, data: VisitData): VisitResult {
            val newWhen = processWhen(expression, data, expressionVisitor, this) { visitResult, original ->
                visitResult.runIfChangedOrDefault(listOf(original)) { statements }
            }

            if (newWhen != expression) {
                return DecomposedResult(newWhen, unitValue)
            }
            return KeptResult
        }

        private inner class BreakContinueUpdater(val breakLoop: IrLoop, val continueLoop: IrLoop) : IrElementTransformer<IrLoop> {
            override fun visitBreak(jump: IrBreak, data: IrLoop) = jump.apply {
                if (loop == data) loop = breakLoop
            }

            override fun visitContinue(jump: IrContinue, data: IrLoop) = jump.apply {
                if (loop == data) loop = continueLoop
            }
        }

        // while (c_block {}) {
        //  body {}
        // }
        //
        // is transformed into
        //
        // while (true) {
        //   var cond = c_block {}
        //   if (!cond) break
        //   body {}
        // }
        //
        override fun visitWhileLoop(loop: IrWhileLoop, data: VisitData): VisitResult {

            val conditionResult = loop.condition.accept(expressionVisitor, data)
            val bodyResult = loop.body?.accept(this, data)
            val unitType = context.builtIns.unitType

            return conditionResult.runIfChanged {
                bodyResult?.run { assert(status == VisitStatus.KEPT) }

                val condVariable = conditionResult.resultValue
                val thenBlock = JsIrBuilder.buildBlock(unitType, listOf(JsIrBuilder.buildBreak(unitType, loop)))
                val breakCond = JsIrBuilder.buildCall(context.irBuiltIns.booleanNotSymbol).apply {
                    putValueArgument(0, condVariable)
                }

                statements.add(JsIrBuilder.buildIfElse(unitType, breakCond, thenBlock))

                val oldBody = loop.body!!

                val newBody = statements + oldBody

                val newLoop = IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
                    label = loop.label
                    condition = constTrue
                    body = oldBody.run { IrBlockImpl(startOffset, endOffset, type, origin, newBody) }
                }

                newLoop.body?.transform(BreakContinueUpdater(newLoop, newLoop), loop)

                DecomposedResult(newLoop, unitValue)
            }
        }

        // do  {
        //  body {}
        // } while (c_block {})
        //
        // is transformed into
        //
        // do {
        //   do {
        //     body {}
        //   } while (false)
        //   cond = c_block {}
        // } while (cond)
        //
        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: VisitData): VisitResult {

            val bodyResult = loop.body?.accept(this, data)
            val conditionResult = loop.condition.accept(expressionVisitor, data)
            val unitType = context.builtIns.unitType

            return conditionResult.runIfChanged {
                bodyResult?.run { assert(status == VisitStatus.KEPT) }

                val body = loop.body!!

                val innerLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin, body, constFalse).apply {
                    label = makeLoopLabel()
                }

                val condVariable = conditionResult.resultValue
                val newBody = JsIrBuilder.buildBlock(body.type, listOf(innerLoop) + statements)
                val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin, newBody, condVariable).apply {
                    label = loop.label ?: makeLoopLabel()
                }

                body.transform(BreakContinueUpdater(newLoop, innerLoop), loop)

                DecomposedResult(newLoop, unitValue)
            }
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: VisitData): VisitResult {
            val argumentResult = expression.argument.accept(expressionVisitor, data)

            return argumentResult.runIfChanged {
                val newOperator = IrTypeOperatorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.operator,
                    expression.typeOperand,
                    resultValue,
                    expression.typeOperandClassifier
                )

                val resValue: IrExpression

                if (!KotlinBuiltIns.isUnit(expression.type)) {
                    val resVar = makeTempVar(expression.type)
                    statements += JsIrBuilder.buildVar(resVar, newOperator)
                    resValue = JsIrBuilder.buildGetValue(resVar)
                } else {
                    statements += newOperator
                    resValue = unitValue
                }

                DecomposedResult(statements, resValue)
            }
        }

        override fun visitField(declaration: IrField, data: VisitData): VisitResult {
            if (declaration.initializer == null) return KeptResult

            val result = declaration.initializer!!.accept(expressionVisitor, data)

            return result.runIfChanged {
                TODO()
            }
        }

        override fun visitSetField(expression: IrSetField, data: VisitData): VisitResult {
            var needNew = false

            val receiverResult = expression.receiver?.accept(expressionVisitor, data)
            val valueResult = expression.value.accept(expressionVisitor, data)

            val newStatements = mutableListOf<IrStatement>()

            val newReceiver = receiverResult?.runIfChangedOrDefault(expression.receiver) {
                newStatements += statements
                needNew = true
                resultValue
            }

            val newValue = valueResult.runIfChangedOrDefault(expression.value) {
                newStatements += statements
                needNew = true
                resultValue
            }

            if (needNew) {
                newStatements += JsIrBuilder.buildSetField(expression.symbol, newReceiver, newValue, expression.superQualifierSymbol)
                return DecomposedResult(newStatements, unitValue)
            }

            return KeptResult
        }

        override fun visitSetVariable(expression: IrSetVariable, data: VisitData): VisitResult {
            val valueResult = expression.value.accept(expressionVisitor, data)

            return valueResult.runIfChanged {
                statements += JsIrBuilder.buildSetVariable(expression.symbol, resultValue)
                DecomposedResult(statements, unitValue)
            }
        }
    }

    inner class ExpressionVisitor : DecomposerVisitor() {
        override fun visitExpression(expression: IrExpression, data: VisitData) = KeptResult

        //
        // val x = block {
        //   ...
        //   expr
        // }
        //
        // is transformed into
        //
        // val x_tmp
        // block {
        //   ...
        //   x_tmp = expr
        // }
        // val x = x_tmp
        override fun visitContainerExpression(expression: IrContainerExpression, data: VisitData): VisitResult {
            val variable = makeTempVar(expression.type)
            val varDeclaration = JsIrBuilder.buildVar(variable)

            val blockStatements = expression.statements
            val lastStatement: IrStatement? = blockStatements.lastOrNull()

            val body = when (expression) {
                is IrBlock -> IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType,
                    expression.origin
                )
                is IrComposite -> IrCompositeImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType,
                    expression.origin
                )
                else -> error("Unsupported block type")
            }

            val collectingList = body.statements

            for (i in 0 until blockStatements.size - 1) {
                val statement = blockStatements[i]
                val tempResult = statement.accept(statementVisitor, data)
                collectingList += tempResult.runIfChangedOrDefault(listOf(statement)) { statements }
            }

            if (lastStatement != null) {
                val result = lastStatement.accept(expressionVisitor, data).runIfChangedOrDefault(lastStatement as IrExpression) {
                    collectingList += statements
                    resultValue
                }
                collectingList += JsIrBuilder.buildSetVariable(variable, result)
                return DecomposedResult(mutableListOf(varDeclaration, body), JsIrBuilder.buildGetValue(variable))
            } else {
                // do not allow variable to be uninitialized
                return DecomposedResult(mutableListOf(), unitValue)
            }
        }

        override fun visitGetClass(expression: IrGetClass, data: VisitData): VisitResult {
            val res = super.visitGetClass(expression, data)

            return res.runIfChanged { DecomposedResult(statements, JsIrBuilder.buildGetClass(resultValue, expression.type)) }
        }

        override fun visitGetField(expression: IrGetField, data: VisitData): VisitResult {
            val res = super.visitGetField(expression, data)

            return res.runIfChanged {
                DecomposedResult(statements, JsIrBuilder.buildGetField(expression.symbol, resultValue, expression.superQualifierSymbol))
            }
        }

        private fun prepareArgument(arg: IrExpression, needWrap: Boolean, statements: MutableList<IrStatement>): IrExpression {
            return if (needWrap) {
                val wrapVar = makeTempVar(arg.type)
                statements += JsIrBuilder.buildVar(wrapVar, arg)
                JsIrBuilder.buildGetValue(wrapVar)
            } else arg
        }

        private fun mapArguments(
            argResults: List<Pair<IrExpression?, VisitResult?>>,
            toDecompose: Int,
            newStatements: MutableList<IrStatement>
        ): List<IrExpression?> {
            var decomposed = 0
            return argResults.map { (original, result) ->
                val needWrap = decomposed < toDecompose
                original?.let {
                    val evaluated = result!!.runIfChangedOrDefault(it) {
                        newStatements += statements
                        decomposed++
                        resultValue
                    }
                    prepareArgument(evaluated, needWrap, newStatements)
                }
            }
        }

        // The point here is to keep original evaluation order so (there is the same story for StringConcat)
        // d.foo(p1, p2, block {}, p4, block {}, p6, p7)
        //
        // is transformed into
        //
        // var d_tmp = d
        // var p1_tmp = p1
        // var p2_tmp = p2
        // var p3_tmp = block {}
        // var p4_tmp = p4
        // var p5_tmp = block {}
        // d_tmp.foo(p1_tmp, p2_tmp, p3_tmp, p4_tmp, p5_tmp, p6, p7)
        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: VisitData): VisitResult {
            var argsCounter = 0

            val dispatchResult = expression.dispatchReceiver?.accept(this, data)?.applyIfChanged { argsCounter++ }
            val extensionResult = expression.extensionReceiver?.accept(this, data)?.applyIfChanged { argsCounter++ }

            val argumentResults = mutableListOf<Pair<IrExpression?, VisitResult?>>().also {
                for (i in 0 until expression.valueArgumentsCount) {
                    val arg = expression.getValueArgument(i)
                    val result = arg?.accept(this, data)?.applyIfChanged { argsCounter++ }
                    it += Pair(arg, result)
                }
            }

            if (argsCounter == 0) return KeptResult

            val newStatements = mutableListOf<IrStatement>()

            val needWrapDR = 0 != argsCounter
            val newDispatchReceiverValue = dispatchResult?.runIfChangedOrDefault(expression.dispatchReceiver) {
                newStatements += statements
                argsCounter--
                resultValue
            }
            val newDispatchReceiver = newDispatchReceiverValue?.let { prepareArgument(it, needWrapDR, newStatements) }

            val needWrapER = 0 != argsCounter
            val newExtensionReceiverValue = extensionResult?.runIfChangedOrDefault(expression.extensionReceiver) {
                newStatements += statements
                argsCounter--
                resultValue
            }
            val newExtensionReceiver = newExtensionReceiverValue?.let { prepareArgument(it, needWrapER, newStatements) }

            val newArguments = mapArguments(argumentResults, argsCounter, newStatements)

            expression.dispatchReceiver = newDispatchReceiver
            expression.extensionReceiver = newExtensionReceiver

            newArguments.forEachIndexed { i, v -> expression.putValueArgument(i, v) }

            val resultVar = makeTempVar(expression.type)

            // TODO: get rid of temporary variable
            newStatements += JsIrBuilder.buildVar(resultVar, expression)

            return DecomposedResult(newStatements, JsIrBuilder.buildGetValue(resultVar))
        }

        override fun visitWhen(expression: IrWhen, data: VisitData): VisitResult {
            val collectiveVar = makeTempVar(expression.type)
            val varDeclaration = JsIrBuilder.buildVar(collectiveVar)
            val newWhen = processWhen(expression, data, this, this) { visitResult, original ->
                val resultList = mutableListOf<IrStatement>()
                val newResult = visitResult.runIfChangedOrDefault(original as IrExpression) {
                    resultList += statements
                    resultValue
                }
                resultList.apply { add(JsIrBuilder.buildSetVariable(collectiveVar, newResult)) }
            }

            if (newWhen != expression) {
                return DecomposedResult(mutableListOf(varDeclaration, newWhen), JsIrBuilder.buildGetValue(collectiveVar))
            }
            return KeptResult
        }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: VisitData): VisitResult {
            var decomposed = 0

            val arguments = expression.arguments.map {
                Pair(it, it.accept(this, data).applyIfChanged { decomposed++ })
            }

            if (decomposed == 0) return KeptResult

            val newStatements = mutableListOf<IrStatement>()
            val newArguments = mapArguments(arguments, decomposed, newStatements)
            val newExpression =
                IrStringConcatenationImpl(expression.startOffset, expression.endOffset, expression.type, newArguments.map { it!! })

            // Unlike to IrCall string concatenation has no side effects itself so it is possible to pass it as return value
            return DecomposedResult(newStatements, newExpression)
        }

        private fun <T : IrStatement> transformTermination(
            value: IrExpression,
            instantiater: (value: IrExpression) -> T,
            data: VisitData
        ): TerminatedResult {
            val valueResult = value.accept(this, data)
            val newStatements = mutableListOf<IrStatement>()

            val returnValue = valueResult.runIfChangedOrDefault(value) {
                newStatements += statements
                resultValue
            }

            newStatements += instantiater(returnValue)

            return TerminatedResult(newStatements, JsIrBuilder.buildCall(unreachableFunction))
        }


        override fun visitReturn(expression: IrReturn, data: VisitData) = transformTermination(
            expression.value,
            { v -> IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, expression.returnTargetSymbol, v) },
            data
        )

        override fun visitThrow(expression: IrThrow, data: VisitData) = transformTermination(
            expression.value,
            { v -> IrThrowImpl(expression.startOffset, expression.endOffset, expression.type, v) },
            data
        )

        override fun visitBreakContinue(jump: IrBreakContinue, data: VisitData): VisitResult {
            return DecomposedResult(jump, JsIrBuilder.buildCall(unreachableFunction))
        }

    }

    fun makeTempVar(type: KotlinType) =
        JsSymbolBuilder.buildTempVar(function.symbol, type, "tmp\$dcms\$${tmpVarCounter++}", true)

    fun makeLoopLabel() = "\$l\$${tmpVarCounter++}"

    private fun processWhen(
        expression: IrWhen,
        data: VisitData,
        expressionVisitor: DecomposerVisitor,
        statementVisitor: DecomposerVisitor,
        bodyBuilder: (VisitResult, IrStatement) -> List<IrStatement>
    ): IrStatement {
        var needNewConds = false
        var needNewBodies = false

        val branches = expression.branches.map {
            val conditionResult = it.condition.accept(expressionVisitor, data).applyIfChanged { needNewConds = true }
            val bodyResult = it.result.accept(statementVisitor, data).applyIfChanged { needNewBodies = true }
            Pair(conditionResult, bodyResult)
        }

        // keep it as is
        if (!(needNewConds || needNewBodies)) return expression

        val unitType = context.builtIns.unitType

        if (needNewConds) {

            // [val x = ] when {
            //  c1_block {} -> b1_block {}
            //  ....
            //  cn_block {} -> bn_block {}
            //  else -> else_block {}
            // }
            //
            // transformed into if-else chain
            // [var x_tmp]
            // val c1 = c1_block {}
            // if (c1) {
            //   [x_tmp =] b1_block {}
            // } else {
            //   val c2 = c2_block {}
            //   if (c2) {
            //     [x_tmp =] b2_block{}
            //   } else {
            //         ...
            //           else {
            //              [x_tmp =] else_block {}
            //           }
            // }
            // [val x = x_tmp]
            //
            val block = IrBlockImpl(expression.startOffset, expression.endOffset, unitType, expression.origin)

            branches.foldIndexed(block) { i, appendBlock, (condResult, bodyResult) ->
                val originalBranch = expression.branches[i]
                val originalCondition = originalBranch.condition
                val originalResult = originalBranch.result

                val irCondition = condResult.runIfChangedOrDefault(originalCondition) {
                    appendBlock.statements += statements
                    resultValue
                }

                val thenBlock = IrBlockImpl(originalResult.startOffset, originalResult.endOffset, unitType).apply {
                    statements += bodyBuilder(bodyResult, originalResult)
                }

                JsIrBuilder.buildBlock(unitType).also {
                    val elseBlock = if (originalBranch is IrElseBranch) null else it

                    val ifElseNode = IrIfThenElseImpl(
                        originalBranch.startOffset,
                        originalBranch.endOffset,
                        unitType,
                        irCondition,
                        thenBlock,
                        elseBlock,
                        expression.origin
                    )
                    appendBlock.statements += ifElseNode
                }
            }

            return block
        }

        // [val x = ] when {
        //  c1 -> b1_block {}
        //  ....
        //  cn -> bn_block {}
        //  else -> else_block {}
        // }
        //
        // transformed into if-else chain
        //
        // [var x_tmp]
        // when {
        //   c1 -> [x_tmp =] b1_block {}
        //   ...
        //   cn -> [x_tmp =] bn_block {}
        //   else -> [x_tmp =] else_block {}
        // }
        // [val x = x_tmp]

        val newWhen = IrWhenImpl(expression.startOffset, expression.endOffset, unitType, expression.origin)

        branches.forEachIndexed { i, (_, bodyResult) ->
            val originalBranch = expression.branches[i]
            val condition = originalBranch.condition
            val originalResult = originalBranch.result

            val body = IrBlockImpl(originalResult.startOffset, originalResult.endOffset, unitType).apply {
                statements += bodyBuilder(bodyResult, originalResult)
            }

            newWhen.branches += when (originalBranch) {
                is IrElseBranch -> IrElseBranchImpl(originalBranch.startOffset, originalBranch.endOffset, condition, body)
                else /* IrBranch */ -> IrBranchImpl(originalBranch.startOffset, originalBranch.endOffset, condition, body)
            }
        }

        return newWhen
    }
}
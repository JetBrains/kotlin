/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addIfNotNull

class BlockDecomposerLowering(context: JsIrBackendContext) : DeclarationContainerLoweringPass {

    private val decomposerTransformer = BlockDecomposerTransformer(context)
    private val nothingType = context.irBuiltIns.nothingType

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat { declaration ->
            when (declaration) {
                is IrFunction -> {
                    lower(declaration)
                    listOf(declaration)
                }
                is IrField -> lower(declaration, irDeclarationContainer)
                else -> listOf(declaration)
            }
        }
    }

    fun lower(irFunction: IrFunction) {
        (irFunction.body as? IrExpressionBody)?.apply {
            irFunction.body = toBlockBody(irFunction)
        }
        irFunction.accept(decomposerTransformer, null)
    }

    private fun IrExpressionBody.toBlockBody(containingFunction: IrFunction): IrBlockBody {
        expression.patchDeclarationParents(containingFunction)
        val returnStatement = JsIrBuilder.buildReturn(containingFunction.symbol, expression, nothingType)
        return IrBlockBodyImpl(expression.startOffset, expression.endOffset).apply {
            statements += returnStatement
        }
    }

    fun lower(irField: IrField, container: IrDeclarationContainer): List<IrDeclaration> {
        irField.initializer?.apply {
            val initFunction = JsIrBuilder.buildFunction(
                irField.name.asString() + "\$init\$",
                expression.type,
                container,
                irField.visibility
            )

            val newBody = toBlockBody(initFunction)
            newBody.patchDeclarationParents(initFunction)
            initFunction.body = newBody

            lower(initFunction)

            val lastStatement = newBody.statements.last()
            if (newBody.statements.size > 1 || lastStatement !is IrReturn || lastStatement.value != expression) {
                expression = JsIrBuilder.buildCall(initFunction.symbol, expression.type)
                return listOf(initFunction, irField)
            }
        }

        return listOf(irField)
    }
}

class BlockDecomposerTransformer(private val context: JsIrBackendContext) : IrElementTransformerVoid() {
    private lateinit var function: IrFunction
    private var tmpVarCounter: Int = 0

    private val statementTransformer = StatementTransformer()
    private val expressionTransformer = ExpressionTransformer()

    private val constTrue get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
    private val constFalse get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)
    private val nothingType = context.irBuiltIns.nothingNType

    private val unitType = context.irBuiltIns.unitType
    private val unitValue get() = JsIrBuilder.buildGetObjectValue(unitType, context.symbolTable.referenceClass(context.builtIns.unit))

    private val unreachableFunction = context.intrinsics.unreachable
    private val booleanNotSymbol = context.irBuiltIns.booleanNotSymbol

    override fun visitFunction(declaration: IrFunction): IrStatement {
        function = declaration
        tmpVarCounter = 0
        return declaration.transform(statementTransformer, null)
    }

    override fun visitElement(element: IrElement) = element.transform(statementTransformer, null)

    private fun processStatements(statements: MutableList<IrStatement>) {
        statements.transformFlat {
            destructureComposite(it.transform(statementTransformer, null))
        }
    }

    private fun makeTempVar(type: IrType, init: IrExpression? = null) =
        JsIrBuilder.buildVar(type, function, initializer = init, isVar = true)

    private fun makeLoopLabel() = "\$l\$${tmpVarCounter++}"

    private fun IrStatement.asExpression(last: IrExpression): IrExpression {
        val composite = JsIrBuilder.buildComposite(last.type)
        composite.statements += transform(statementTransformer, null)
        composite.statements += last
        return composite
    }

    private fun materializeLastExpression(composite: IrComposite, block: (IrExpression) -> IrStatement): IrComposite {
        val statements = composite.statements
        val expression = statements.lastOrNull() as? IrExpression ?: return composite
        statements[statements.lastIndex] = block(expression)
        return composite
    }

    private fun destructureComposite(expression: IrStatement) = (expression as? IrComposite)?.statements ?: listOf(expression)

    private inner class BreakContinueUpdater(val breakLoop: IrLoop, val continueLoop: IrLoop) : IrElementTransformer<IrLoop> {
        override fun visitBreak(jump: IrBreak, data: IrLoop) = jump.apply {
            if (loop == data) loop = breakLoop
        }

        override fun visitContinue(jump: IrContinue, data: IrLoop) = jump.apply {
            if (loop == data) loop = continueLoop
        }
    }

    private inner class StatementTransformer : IrElementTransformerVoid() {
        override fun visitBlockBody(body: IrBlockBody) = body.apply { processStatements(statements) }

        override fun visitContainerExpression(expression: IrContainerExpression) = expression.apply { processStatements(statements) }

        override fun visitExpression(expression: IrExpression) = expression.transform(expressionTransformer, null)

        override fun visitReturn(expression: IrReturn): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.value as? IrComposite ?: return expression
            return materializeLastExpression(composite) {
                IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, expression.returnTargetSymbol, it)
            }
        }

        override fun visitThrow(expression: IrThrow): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.value as? IrComposite ?: return expression
            return materializeLastExpression(composite) {
                IrThrowImpl(expression.startOffset, expression.endOffset, expression.type, it)
            }
        }

        override fun visitBreakContinue(jump: IrBreakContinue) = jump

        override fun visitVariable(declaration: IrVariable): IrStatement {
            declaration.transformChildrenVoid(expressionTransformer)

            val composite = declaration.initializer as? IrComposite ?: return declaration
            return materializeLastExpression(composite) {
                declaration.apply { initializer = it }
            }
        }

        override fun visitSetField(expression: IrSetField): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val receiverResult = expression.receiver as? IrComposite
            val valueResult = expression.value as? IrComposite
            if (receiverResult == null && valueResult == null) return expression

            if (receiverResult != null && valueResult != null) {
                val result = IrCompositeImpl(receiverResult.startOffset, expression.endOffset, unitType)
                val receiverValue = receiverResult.statements.last() as IrExpression
                val irVar = makeTempVar(receiverValue.type, receiverValue)
                val setValue = valueResult.statements.last() as IrExpression
                result.statements += receiverResult.statements.run { subList(0, lastIndex) }
                result.statements += irVar
                result.statements += valueResult.statements.run { subList(0, lastIndex) }
                result.statements += expression.run {
                    IrSetFieldImpl(
                        startOffset,
                        endOffset,
                        symbol,
                        JsIrBuilder.buildGetValue(irVar.symbol),
                        setValue,
                        type,
                        origin,
                        superQualifierSymbol
                    )
                }
                return result
            }

            if (receiverResult != null) {
                return materializeLastExpression(receiverResult) {
                    expression.run {
                        IrSetFieldImpl(startOffset, endOffset, symbol, it, value, type, origin, superQualifierSymbol)
                    }
                }
            }

            assert(valueResult != null)

            val receiver = expression.receiver?.let {
                val irVar = makeTempVar(it.type, it)
                valueResult!!.statements.add(0, irVar)
                JsIrBuilder.buildGetValue(irVar.symbol)
            }

            return materializeLastExpression(valueResult!!) {
                expression.run {
                    IrSetFieldImpl(startOffset, endOffset, symbol, receiver, it, type, origin, superQualifierSymbol)
                }
            }
        }

        override fun visitSetVariable(expression: IrSetVariable): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.value as? IrComposite ?: return expression

            return materializeLastExpression(composite) {
                expression.run { IrSetVariableImpl(startOffset, endOffset, type, symbol, it, origin) }
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
        override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
            val newBody = loop.body?.transform(statementTransformer, null)
            val newCondition = loop.condition.transform(expressionTransformer, null)

            if (newCondition is IrComposite) {
                val newLoopBody = IrBlockImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin)

                newLoopBody.statements += newCondition.statements.run { subList(0, lastIndex) }
                val thenBlock = JsIrBuilder.buildBlock(unitType, listOf(JsIrBuilder.buildBreak(unitType, loop)))

                val newLoopCondition = newCondition.statements.last() as IrExpression

                val breakCond = JsIrBuilder.buildCall(booleanNotSymbol).apply {
                    dispatchReceiver = newLoopCondition
                }

                newLoopBody.statements += JsIrBuilder.buildIfElse(unitType, breakCond, thenBlock)
                newLoopBody.statements.addIfNotNull(newBody)

                return loop.apply {
                    condition = constTrue
                    body = newLoopBody
                }
            }

            return loop.apply {
                body = newBody
                condition = newCondition
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
        override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
            val newBody = loop.body?.transform(statementTransformer, null)
            val newCondition = loop.condition.transform(expressionTransformer, null)

            if (newCondition is IrComposite) {
                val innerLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin).apply {
                    condition = constFalse
                    body = newBody
                    label = makeLoopLabel()
                }

                val newLoopCondition = newCondition.statements.last() as IrExpression
                val newLoopBody = IrBlockImpl(
                    newCondition.startOffset,
                    newBody?.endOffset ?: newCondition.endOffset,
                    newBody?.type ?: unitType
                ).apply {
                    statements += innerLoop
                    statements += newCondition.statements.run { subList(0, lastIndex) }
                }

                val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin)

                return newLoop.apply {
                    condition = newLoopCondition
                    body = newLoopBody.transform(BreakContinueUpdater(newLoop, innerLoop), loop)
                    label = loop.label ?: makeLoopLabel()
                }
            }

            return loop.apply {
                body = newBody
                condition = newCondition
            }
        }

        // when {
        //  c1_block {} -> b1_block {}
        //  ....
        //  cn_block {} -> bn_block {}
        //  else -> else_block {}
        // }
        //
        // transformed into if-else chain
        // c1 = c1_block {}
        // if (c1) {
        //   b1_block {}
        // } else {
        //   c2 = c2_block {}
        //   if (c2) {
        //     b2_block{}
        //   } else {
        //         ...
        //           else {
        //              else_block {}
        //           }
        // }
        override fun visitWhen(expression: IrWhen): IrExpression {

            var compositeCount = 0

            val results = expression.branches.map {
                val cond = it.condition.transform(expressionTransformer, null)
                val res = it.result.transform(statementTransformer, null)
                if (cond is IrComposite) compositeCount++
                Triple(cond, res, it)
            }

            if (compositeCount == 0) {
                val branches = results.map { (cond, res, orig) ->
                    when {
                        isElseBranch(orig) -> IrElseBranchImpl(orig.startOffset, orig.endOffset, cond, res)
                        else /* IrBranch */ -> IrBranchImpl(orig.startOffset, orig.endOffset, cond, res)
                    }
                }
                return expression.run { IrWhenImpl(startOffset, endOffset, type, origin, branches) }
            }

            val block = IrBlockImpl(expression.startOffset, expression.endOffset, unitType, expression.origin)

            // TODO: consider decomposing only when it is really required
            results.fold(block) { appendBlock, (cond, res, orig) ->
                val condStatements = destructureComposite(cond)
                val condValue = condStatements.last() as IrExpression

                appendBlock.statements += condStatements.run { subList(0, lastIndex) }

                JsIrBuilder.buildBlock(unitType).also {
                    val elseBlock = if (isElseBranch(orig)) null else it
                    val ifElseNode =
                        JsIrBuilder.buildIfElse(orig.startOffset, orig.endOffset, unitType, condValue, res, elseBlock, expression.origin)
                    appendBlock.statements += ifElseNode
                }
            }

            return block
        }

        override fun visitTry(aTry: IrTry) = aTry.also { it.transformChildrenVoid(this) }
    }

    private inner class ExpressionTransformer : IrElementTransformerVoid() {

        override fun visitExpression(expression: IrExpression) = expression.transformChildren()

        override fun visitGetField(expression: IrGetField): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.receiver as? IrComposite ?: return expression

            return materializeLastExpression(composite) {
                expression.run { IrGetFieldImpl(startOffset, endOffset, symbol, type, it, origin, superQualifierSymbol) }
            }
        }

        override fun visitGetClass(expression: IrGetClass): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.argument as? IrComposite ?: return expression

            return materializeLastExpression(composite) {
                expression.run { IrGetClassImpl(startOffset, endOffset, type, it) }
            }
        }

        override fun visitLoop(loop: IrLoop) = loop.asExpression(unitValue)

        override fun visitSetVariable(expression: IrSetVariable) = expression.asExpression(unitValue)

        override fun visitSetField(expression: IrSetField) = expression.asExpression(unitValue)

        override fun visitBreakContinue(jump: IrBreakContinue) =
            jump.asExpression(JsIrBuilder.buildCall(unreachableFunction.symbol, nothingType))

        override fun visitThrow(expression: IrThrow) =
            expression.asExpression(JsIrBuilder.buildCall(unreachableFunction.symbol, nothingType))

        override fun visitReturn(expression: IrReturn) =
            expression.asExpression(JsIrBuilder.buildCall(unreachableFunction.symbol, nothingType))

        override fun visitVariable(declaration: IrVariable) = declaration.asExpression(unitValue)

        override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val compositeCount = expression.arguments.count { it is IrComposite }

            if (compositeCount == 0) return expression

            val newStatements = mutableListOf<IrStatement>()
            val arguments = mapArguments(expression.arguments, compositeCount, newStatements)

            newStatements += expression.run { IrStringConcatenationImpl(startOffset, endOffset, type, arguments.map { it!! }) }
            return JsIrBuilder.buildComposite(expression.type, newStatements)
        }

        private fun mapArguments(
            oldArguments: Collection<IrExpression?>,
            compositeCount: Int,
            newStatements: MutableList<IrStatement>,
            dontDetachFirstArgument: Boolean = false
        ): List<IrExpression?> {
            var compositesLeft = compositeCount
            val arguments = mutableListOf<IrExpression?>()

            for ((index, arg) in oldArguments.withIndex()) {
                val value = if (arg is IrComposite) {
                    compositesLeft--
                    newStatements += arg.statements.run { subList(0, lastIndex) }
                    arg.statements.last() as IrExpression
                } else arg

                val newArg = when {
                    compositesLeft == 0 -> value
                    index == 0 && dontDetachFirstArgument -> value
                    value == null -> value
                    else -> {
                        // TODO: do not wrap if value is pure (const, variable, etc)
                        val irVar = makeTempVar(value.type, value)
                        newStatements += irVar
                        JsIrBuilder.buildGetValue(irVar.symbol)
                    }
                }

                arguments += newArg
            }
            return arguments
        }

        // TODO: remove this when vararg is lowered
        override fun visitVararg(expression: IrVararg): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val compositeCount = expression.elements.count { it is IrComposite }

            if (compositeCount == 0) return expression

            val newStatements = mutableListOf<IrStatement>()
            val argumentsExpressions = mapArguments(
                expression.elements.map { (it as? IrSpreadElement)?.expression ?: it as IrExpression },
                compositeCount,
                newStatements
            )

            val arguments = expression.elements.withIndex().map { (i, v) ->
                val expr = argumentsExpressions[i]!!
                (v as? IrSpreadElement)?.run { IrSpreadElementImpl(startOffset, endOffset, expr) } ?: expr
            }

            newStatements += expression.run { IrVarargImpl(startOffset, endOffset, type, varargElementType, arguments) }
            return expression.run { IrCompositeImpl(startOffset, endOffset, type, null, newStatements) }
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
        override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val oldArguments = mutableListOf(expression.dispatchReceiver, expression.extensionReceiver)
            for (i in 0 until expression.valueArgumentsCount) oldArguments += expression.getValueArgument(i)
            val compositeCount = oldArguments.count { it is IrComposite }

            if (compositeCount == 0) return expression

            val newStatements = mutableListOf<IrStatement>()
            val newArguments = mapArguments(oldArguments, compositeCount, newStatements)

            expression.dispatchReceiver = newArguments[0]
            expression.extensionReceiver = newArguments[1]

            for (i in 0 until expression.valueArgumentsCount) {
                expression.putValueArgument(i, newArguments[i + 2])
            }

            newStatements += expression

            return expression.run { IrCompositeImpl(startOffset, endOffset, type, origin, newStatements) }
        }

        override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val composite = expression.receiver as? IrComposite ?: return expression

            return materializeLastExpression(composite) {
                expression.apply { receiver = it }
            }
        }

        override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression): IrExpression {
            expression.transformChildrenVoid(expressionTransformer)

            val oldArguments = listOf(expression.receiver) + expression.arguments
            val compositeCount = oldArguments.count { it is IrComposite }

            if (compositeCount == 0) return expression

            val newStatements = mutableListOf<IrStatement>()
            val newArguments = mapArguments(
                oldArguments,
                compositeCount,
                newStatements,
                dontDetachFirstArgument = expression.isReceiverNonDetachable()
            )

            expression.receiver = newArguments[0]
                ?: error("No new receiver in destructured composite for:\n${expression.dump()}")

            for (i in expression.arguments.indices) {
                expression.arguments[i] = newArguments[i + 1]
                    ?: error("No argument #$i in destructured composite for:\n${expression.dump()}")
            }

            newStatements.add(expression)

            return expression.run { IrCompositeImpl(startOffset, endOffset, type, null, newStatements) }
        }

        // Return if receiver expression cannot be detached from this expression
        private fun IrDynamicOperatorExpression.isReceiverNonDetachable(): Boolean {
            val receiver = when (val r = this.receiver) {
                is IrComposite -> r.statements.lastOrNull() ?: return false
                else -> r
            }

            val receiverIsMemberAccess =
                receiver is IrDynamicMemberExpression ||
                        (receiver is IrDynamicOperatorExpression && receiver.operator == IrDynamicOperator.ARRAY_ACCESS)

            val operatorDependsOnMemberAccess = (operator == IrDynamicOperator.INVOKE)

            return operator.isAssignmentOperator || (receiverIsMemberAccess && operatorDependsOnMemberAccess)
        }

        override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {

            expression.run { if (statements.isEmpty()) return IrCompositeImpl(startOffset, endOffset, type, origin, listOf(unitValue)) }

            val newStatements = mutableListOf<IrStatement>()

            for (i in 0 until expression.statements.lastIndex) {
                newStatements += destructureComposite(expression.statements[i].transform(statementTransformer, null))
            }

            newStatements += destructureComposite(expression.statements.last().transform(expressionTransformer, null))

            return JsIrBuilder.buildComposite(expression.type, newStatements)
        }

        private fun wrap(expression: IrExpression) =
            expression as? IrBlock ?: expression.let { IrBlockImpl(it.startOffset, it.endOffset, it.type, null, listOf(it)) }

        private fun wrap(expression: IrExpression, variable: IrVariable) =
            wrap(JsIrBuilder.buildSetVariable(variable.symbol, expression, unitType))

        // try {
        //   try_block {}
        // } catch () {
        //   catch_block {}
        // } finally {}
        //
        // transformed into if-else chain
        //
        // Composite [
        //   var tmp
        //   try {
        //     tmp = try_block {}
        //   } catch () {
        //     tmp = catch_block {}
        //   } finally {}
        //   tmp
        // ]
        override fun visitTry(aTry: IrTry): IrExpression {
            val irVar = makeTempVar(aTry.type)

            val newTryResult = wrap(aTry.tryResult, irVar)
            val newCatches = aTry.catches.map {
                val newCatchBody = wrap(it.result, irVar)
                IrCatchImpl(it.startOffset, it.endOffset, it.catchParameter, newCatchBody)
            }

            val newTry = aTry.run { IrTryImpl(startOffset, endOffset, unitType, newTryResult, newCatches, finallyExpression) }
            newTry.transformChildrenVoid(statementTransformer)

            val newStatements = listOf(irVar, newTry, JsIrBuilder.buildGetValue(irVar.symbol))
            return JsIrBuilder.buildComposite(aTry.type, newStatements)
        }

        // when {
        //  c1 -> b1_block {}
        //  ....
        //  cn -> bn_block {}
        //  else -> else_block {}
        // }
        //
        // transformed into if-else chain if anything should be decomposed
        //
        // Composite [
        //   var tmp
        //   when {
        //     c1 -> tmp = b1_block {}
        //     ...
        //     cn -> tmp = bn_block {}
        //     else -> tmp = else_block {}
        //   }
        //   tmp
        // ]
        //
        // kept `as is` otherwise

        override fun visitWhen(expression: IrWhen): IrExpression {

            var hasComposites = false
            val decomposedResults = expression.branches.map {
                val transformedCondition = it.condition.transform(expressionTransformer, null)
                val transformedResult = it.result.transform(expressionTransformer, null)
                hasComposites = hasComposites || transformedCondition is IrComposite || transformedResult is IrComposite
                Triple(it, transformedCondition, transformedResult)
            }

            if (hasComposites) {
                val irVar = makeTempVar(expression.type)

                val newBranches = decomposedResults.map { (branch, condition, result) ->
                    val newResult = wrap(result, irVar)
                    when {
                        isElseBranch(branch) -> IrElseBranchImpl(branch.startOffset, branch.endOffset, condition, newResult)
                        else /* IrBranch  */ -> IrBranchImpl(branch.startOffset, branch.endOffset, condition, newResult)
                    }
                }

                val newWhen =
                    expression.run { IrWhenImpl(startOffset, endOffset, unitType, origin, newBranches) }
                        .transform(statementTransformer, null)  // deconstruct into `if-else` chain

                return JsIrBuilder.buildComposite(expression.type, listOf(irVar, newWhen, JsIrBuilder.buildGetValue(irVar.symbol)))
            } else {
                val newBranches = decomposedResults.map { (branch, condition, result) ->
                    when {
                        isElseBranch(branch) -> IrElseBranchImpl(branch.startOffset, branch.endOffset, condition, result)
                        else /* IrBranch  */ -> IrBranchImpl(branch.startOffset, branch.endOffset, condition, result)
                    }
                }

                return expression.run { IrWhenImpl(startOffset, endOffset, type, origin, newBranches) }
            }
        }
    }
}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WhileConditionFoldingLowering(private val context: JsIrBackendContext) : BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.acceptVoid(Visitor(container))
    }

    private inner class Visitor(private val container: IrDeclaration) : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitWhileLoop(loop: IrWhileLoop) {
            super.visitWhileLoop(loop)
            context.createIrBuilder(container.symbol).process(loop)
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
            super.visitDoWhileLoop(loop)
            val body = loop.body ?: return
            if (!hasContinue(body, loop)) {
                context.createIrBuilder(container.symbol).process(loop)
            }
        }
    }

    private fun DeclarationIrBuilder.process(loop: IrLoop) {
        do {
            val body = loop.body ?: break
            var optimized = false
            val isDoWhile = loop is IrDoWhileLoop
            val first = findFirstNonBlockStatement(body, reverse = isDoWhile) ?: break
            val condition = extractCondition(first, loop)
            if (condition != null) {
                loop.body = removeFirstNonBlockStatement(body, reverse = isDoWhile) ?: irComposite {}
                val existingCondition = loop.condition
                loop.condition = when {
                    existingCondition.isTrueConst() -> condition
                    isDoWhile -> context.andand(condition, existingCondition)
                    else -> context.andand(existingCondition, condition)
                }
                optimized = true
            }
        } while (optimized)
    }

    /**
     * Gets condition from while or do..while body and convert it into while/do..while condition
     *
     * For example,
     *
     *     while (true) {
     *         if (!A) break
     *         B()
     *     }
     *
     * This function should return `A`, (negated condition of if statement).
     */
    private fun DeclarationIrBuilder.extractCondition(statement: IrStatement, loop: IrLoop): IrExpression? = when (statement) {
        is IrBreak if statement.loop == loop -> {
            // Code like this
            //
            //     while (A) {
            //         break;
            //         B();
            //     }
            //
            // can be rewritten as
            //
            //     while (A && false) {
            //         B();
            //     }
            //
            // therefore for single `break` we should return `false`.
            context.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        }
        is IrWhen if statement.branches.size == 1 -> {
            // Code like this
            //
            //     while (A) {
            //         if (!B)
            //             X;
            //         D();
            //     }
            //
            // where X is a statement, and we can extract condition `C` from it, can be rewritten as
            //
            //     while (A && (B || C)) {
            //         D()
            //     }
            // therefore we return B || C
            //
            // an example is
            //
            //     while (A) {
            //         if (!B)
            //             if (!C)
            //                 break;
            //         D()
            //     }
            //
            // applying this rule repeatedly we get while (A && (B || C)), which is correct
            val branch = statement.branches[0]
            extractCondition(branch.result, loop)?.let { nextCondition ->
                if (nextCondition.isFalseConst()) {
                    // Just a little optimization. When inner statement is a single `break`, `nextCondition` would be false.
                    // However, `A || false` can be rewritten as simply `A`
                    not(branch.condition)
                } else {
                    context.oror(not(branch.condition), nextCondition)
                }
            }
        }
        is IrContainerExpression if statement.statements.size == 1 -> extractCondition(statement.statements[0], loop)
        else -> null
    }

    private fun findFirstNonBlockStatement(expression: IrExpression, reverse: Boolean): IrExpression? = when (expression) {
        is IrContainerExpression -> {
            val statements = if (reverse) expression.statements.asReversed() else expression.statements
            for (statement in statements) {
                when (statement) {
                    is IrExpression -> {
                        findFirstNonBlockStatement(statement, reverse)?.let { return it }
                    }
                    else -> break
                }
            }
            return null
        }
        else -> expression
    }


    private fun removeFirstNonBlockStatement(expression: IrExpression, reverse: Boolean): IrExpression? = when (expression) {
        is IrContainerExpression -> {
            val statements = expression.statements
            val indices = if (reverse) statements.indices.reversed() else statements.indices
            for (i in indices) {
                when (val statement = statements[i]) {
                    is IrExpression -> {
                        if (removeFirstNonBlockStatement(statement, reverse) == null) {
                            statements.removeAt(i)
                            return expression
                        }
                    }
                    else -> break
                }
            }
            null
        }
        else -> null
    }

    private fun hasContinue(expression: IrExpression, loop: IrLoop): Boolean {
        var found = false
        expression.acceptVoid(object : IrVisitorVoid() {
            override fun visitContinue(jump: IrContinue) {
                if (jump.loop === loop) {
                    found = true
                }
            }

            override fun visitFunction(declaration: IrFunction) {}

            override fun visitElement(element: IrElement) {
                if (!found) {
                    element.acceptChildrenVoid(this)
                }
            }
        })
        return found
    }


    private fun not(expression: IrExpression): IrExpression = primitiveOp1(
        startOffset = expression.startOffset,
        endOffset = expression.endOffset,
        primitiveOpSymbol = context.symbols.jsNot,
        primitiveOpReturnType = context.irBuiltIns.booleanType,
        origin = IrStatementOrigin.EXCL,
        dispatchReceiver = expression,
    )
}

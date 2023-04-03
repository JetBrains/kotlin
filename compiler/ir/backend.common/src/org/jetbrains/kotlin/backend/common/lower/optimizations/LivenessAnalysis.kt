/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.copy
import org.jetbrains.kotlin.backend.common.forEachBit
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import java.util.*

private fun BitSet.withBit(bit: Int) = if (get(bit)) this else copy().also { it.set(bit) }
private fun BitSet.withOutBit(bit: Int) = if (!get(bit)) this else copy().also { it.clear(bit) }

object LivenessAnalysis {
    fun run(body: IrBody, filter: (IrElement) -> Boolean) =
        LivenessAnalysisVisitor(filter).run(body)

    private fun IrElement.getImmediateChildren(): List<IrElement> {
        val result = mutableListOf<IrElement>()
        acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                result.add(element)
                // Do not recurse.
            }
        })
        return result
    }

    /*
     * The classic liveness analysis algorithm works on CFG and traverses it from bottom to top,
     * this directly translates to the AST traversal from right to left.
     * Each visitXXX takes live variables ~after~ the [element] and returns live variables ~before~ the [element].
     */
    private class LivenessAnalysisVisitor(val filter: (IrElement) -> Boolean) : IrElementVisitor<BitSet, BitSet> {
        private val variables = mutableListOf<IrVariable>()
        private val variableIds = mutableMapOf<IrVariable, Int>()
        private val filteredElementEndsLV = mutableMapOf<IrElement, BitSet>()
        private val returnableBlockEndsLV = mutableMapOf<IrReturnableBlock, BitSet>()
        private val loopEndsLV = mutableMapOf<IrLoop, BitSet>()
        private val loopStartsLV = mutableMapOf<IrLoop, BitSet>()
        private var catchesLV = BitSet()

        fun run(body: IrBody): Map<IrElement, List<IrVariable>> {
            body.accept(this, BitSet() /* No variable is live at the end */)
            return filteredElementEndsLV.mapValues { (_, liveVariables) ->
                buildList { liveVariables.forEachBit { add(variables[it]) } }
            }
        }

        private fun getVariableId(variable: IrVariable) = variableIds.getOrPut(variable) {
            variables.add(variable)
            variables.lastIndex
        }

        private inline fun <T : IrElement> saveAndCompute(element: T, liveVariables: BitSet, compute: () -> BitSet): BitSet {
            if (filter(element))
                filteredElementEndsLV[element] = liveVariables.copy().also { it.or(catchesLV) }
            return compute()
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: BitSet) =
            error("Local declarations should've been popped out by this point")

        // Default: traverse the children in the reverse order, propagating live variables from right to left.
        override fun visitElement(element: IrElement, data: BitSet) = saveAndCompute(element, data) {
            val children = element.getImmediateChildren()
            var liveVariables = data
            for (i in children.size - 1 downTo 0) {
                liveVariables = children[i].accept(this, liveVariables)
            }
            liveVariables
        }

        override fun visitGetValue(expression: IrGetValue, data: BitSet) = saveAndCompute(expression, data) {
            val variable = expression.symbol.owner as? IrVariable ?: return@saveAndCompute data
            data.withBit(getVariableId(variable))
        }

        override fun visitVariable(declaration: IrVariable, data: BitSet) = saveAndCompute(declaration, data) {
            val variableId = getVariableId(declaration)
            var liveVariables = data.withOutBit(variableId)
            liveVariables = declaration.initializer?.accept(this, liveVariables) ?: liveVariables
            require(!liveVariables.get(variableId)) { "Use of uninitialized variable ${declaration.render()}" }
            liveVariables
        }

        override fun visitSetValue(expression: IrSetValue, data: BitSet) = saveAndCompute(expression, data) {
            val variable = expression.symbol.owner as? IrVariable ?: error("Unexpected parameter rewrite: ${expression.render()}")
            val liveVariables = data.withOutBit(getVariableId(variable))
            expression.value.accept(this, liveVariables)
        }

        override fun visitReturn(expression: IrReturn, data: BitSet) = saveAndCompute(expression, data) {
            val liveVariables =
                (expression.returnTargetSymbol.owner as? IrReturnableBlock)?.let {
                    returnableBlockEndsLV[it] ?: error("Unknown return target ${expression.returnTargetSymbol.owner.render()}")
                } ?: BitSet() // No variable is alive after returning from the function.
            expression.value.accept(this, liveVariables)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: BitSet) = saveAndCompute(expression, data) {
            (expression as? IrReturnableBlock)?.let { returnableBlock ->
                returnableBlockEndsLV[returnableBlock] = data
            }
            visitElement(expression, data)
        }

        /*
         *  (when)
         *    |
         *    +-> (cond1)
         *          |  |
         *          |  +--> (res1) --------+
         *          |                      |
         *          +--> (cond2)           |
         *                 |  |            |
         *                 |  +--> (res2) ----+
         *                 |               |  |
         *   ...................................  // more branches
         *                 |               |  |
         *                 v               v  v
         *               ( ....... next ....... )
         */
        override fun visitWhen(expression: IrWhen, data: BitSet) = saveAndCompute(expression, data) {
            val isExhaustive = expression.branches.last().isUnconditional()
            // An inexhaustive when clause can skip all the results and go to (next).
            var liveVariables = if (isExhaustive) BitSet() else data
            for (i in expression.branches.size - 1 downTo 0) {
                val branch = expression.branches[i]
                val conditionEndLV = liveVariables.copy() // (cond) was false.
                conditionEndLV.or(branch.result.accept(this, data)) // (cond) was true.
                liveVariables = branch.condition.accept(this, conditionEndLV)
            }
            liveVariables
        }

        override fun visitThrow(expression: IrThrow, data: BitSet) = saveAndCompute(expression, data) {
            expression.value.accept(this, catchesLV) // Assuming a throw might be caught by one of the nearest catch clauses.
        }

        /*
         * Here instead of precise but much more complicated solution, use simple approximation:
         * assume an exception might be thrown anywhere from inside a try clause, and will be caught
         * by either of the catch clauses. This implies that each alive variable at the start of any
         * catch clauses must be alive anywhere inside the corresponding try clause (and at its start).
         */
        override fun visitTry(aTry: IrTry, data: BitSet) = saveAndCompute(aTry, data) {
            require(aTry.finallyExpression == null) { "All finally clauses should've been lowered" }
            val currentCatchesLV = BitSet() // Live variables after try clause if there was an exception.
            for (aCatch in aTry.catches) {
                currentCatchesLV.or(aCatch.accept(this, data))
            }
            val prevCatchesLV = catchesLV
            catchesLV = currentCatchesLV
            currentCatchesLV.or(aTry.tryResult.accept(this, data))
            catchesLV = prevCatchesLV
            currentCatchesLV
        }

        override fun visitBreak(jump: IrBreak, data: BitSet) = saveAndCompute(jump, data) {
            loopEndsLV[jump.loop] ?: error("Break from an unknown loop ${jump.loop.render()}")
        }

        override fun visitContinue(jump: IrContinue, data: BitSet) = saveAndCompute(jump, data) {
            loopStartsLV[jump.loop] ?: error("Continue to an unknown loop ${jump.loop.render()}")
        }

        /*
         *                 +---- (body) <--+
         *                 |        |      |
         *                 | b      |      |      // continue goes to (cond)
         *  do-while loop: | r      |      |
         *                 | e      v      |
         *                 | a   (cond) ---+
         *                 | k      |
         *                 |        v
         *                 +---> (next)
         */
        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BitSet) = saveAndCompute(loop, data) {
            handleLoop(loop, data)
        }

        /*
         * For while loops we do the following transformation:
         * while (condition) { body } -> if (condition) { do { body } while (condition) }
         *
         *                       (cond) ------+
         *                          |         |
         *                          v         |
         *                 +---- (body) <--+  |
         *                 |        |      |  |
         *                 | b      |      |  |   // continue goes to (cond)
         *  while loop:    | r      |      |  |
         *                 | e      v      |  |
         *                 | a   (cond) ---+  |
         *                 | k      |         |
         *                 |        v         |
         *                 +---> (next) <-----+
         */
        override fun visitWhileLoop(loop: IrWhileLoop, data: BitSet) = saveAndCompute(loop, data) {
            val liveVariables = data.copy() // If no iteration has been executed.
            liveVariables.or(handleLoop(loop, data)) // The actual loop body.
            loop.condition.accept(this, liveVariables)
        }

        private fun handleLoop(loop: IrLoop, data: BitSet): BitSet {
            loopEndsLV[loop] = data
            var bodyEndLV = loop.condition.accept(this, data)
            val body = loop.body ?: return bodyEndLV
            var bodyStartLV: BitSet
            // In practice, only one or two iterations seem to be enough, but the classic algorithm
            // loops until "saturation" (when nothing changes anymore).
            do {
                loopStartsLV[loop] = bodyEndLV
                bodyStartLV = body.accept(this, bodyEndLV)
                val nextBodyEndLV = loop.condition.accept(this, bodyStartLV)
                val lvHaveChanged = nextBodyEndLV != bodyEndLV
                bodyEndLV = nextBodyEndLV
            } while (lvHaveChanged)
            return bodyStartLV
        }
    }
}
/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.lazy

import java.util.HashSet
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.siblings
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.utils.addIfNotNull
import java.util.ArrayList
import java.util.HashMap
import com.intellij.psi.PsiElement
import org.jetbrains.jet.JetNodeTypes
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import org.jetbrains.jet.lang.psi.psiUtil.isProbablyNothing
import org.jetbrains.jet.utils.addToStdlib.firstOrNullIsInstance

//TODO: do resolve anonymous object's body

class PartialBodyResolveFilter(
        elementToResolve: JetElement,
        private val body: JetExpression,
        probablyNothingCallableNamesService: ProbablyNothingCallableNamesService
) : (JetElement) -> Boolean {

    private val statementTree = StatementTree()

    private val nothingFunctionNames = HashSet(probablyNothingCallableNamesService.functionNames())
    private val nothingPropertyNames = probablyNothingCallableNamesService.propertyNames()

    ;{
        assert(body.isAncestor(elementToResolve, strict = false))

        body.accept(object : JetVisitorVoid() {
            override fun visitNamedFunction(function: JetNamedFunction) {
                super.visitNamedFunction(function)

                if (function.getTypeReference().isProbablyNothing()) {
                    nothingFunctionNames.add(function.getName())
                }
            }

            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }
        })

        statementTree.mark(elementToResolve, MarkLevel.NEED_COMPLETION)
        statementTree.blocks(body).forEach {
            processBlock(it)
        }
    }

    //TODO: do..while is special case

    private fun processBlock(block: JetBlockExpression): NameFilter {
        val nameFilter = NameFilter()
        if (!statementTree.hasMarks(block, MarkLevel.NEED_REFERENCE_RESOLVE)) return nameFilter

        val startStatement = block.getLastChild().siblings(forward = false)
                .filterIsInstance<JetExpression>()
                .first { statementTree.statementMark(it) > MarkLevel.RESOLVE_STATEMENT }

        for (statement in startStatement.siblings(forward = false)) {
            if (statement !is JetExpression) continue

            if (statement is JetNamedDeclaration) {
                val name = statement.getName()
                if (name != null && nameFilter.accepts(name)) {
                    statementTree.mark(statement, MarkLevel.NEED_REFERENCE_RESOLVE)
                }
            }
            else if (statement is JetMultiDeclaration) {
                if (statement.getEntries().any {
                    val name = it.getName()
                    name != null && nameFilter.accepts(name)
                }) {
                    statementTree.mark(statement, MarkLevel.NEED_REFERENCE_RESOLVE)
                }
            }

            fun updateNameFilter() {
                val level = statementTree.statementMark(statement)
                when (level) {
                    MarkLevel.NEED_REFERENCE_RESOLVE -> nameFilter.addUsedNames(statement)
                    MarkLevel.NEED_COMPLETION -> nameFilter.addAllNames()
                }
            }

            updateNameFilter()

            if (!nameFilter.isEmpty) {
                val smartCastPlaces = potentialSmartCastPlaces(statement, { it.identifiers().all { nameFilter.accepts(it) } })
                if (!smartCastPlaces.isEmpty()) {
                    //TODO: do we really need correct resolve for ALL smart cast places?
                    smartCastPlaces.values()
                            .flatMap { it }
                            .forEach { statementTree.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
                    updateNameFilter()
                }
            }

            val level = statementTree.statementMark(statement)
            if (level > MarkLevel.RESOLVE_STATEMENT) {
                for (nestedBlock in statementTree.blocks(statement)) {
                    if (isValueNeeded(nestedBlock)) {
                        nestedBlock.lastStatement()?.let { statementTree.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
                    }
                    val childFilter = processBlock(nestedBlock)
                    nameFilter.addNames(childFilter)
                }
            }
        }

        return nameFilter
    }

    override fun invoke(statement: JetElement)
            = statement is JetExpression && statementTree.statementMark(statement) != MarkLevel.NONE

    /**
     * Finds places within the given statement that may affect smart-casts after it.
     * That is, such places whose containing statements must be left in code to keep the smart-casts.
     * Returns map from smart-cast expression names (variable name or qualified variable name) to places.
     */
    private fun potentialSmartCastPlaces(
            statement: JetExpression,
            filter: (SmartCastName) -> Boolean = { true }
    ): Map<SmartCastName, List<JetExpression>> {
        val map = HashMap<SmartCastName, ArrayList<JetExpression>>(0)

        fun addPlace(name: SmartCastName, place: JetExpression) {
            map.getOrPut(name, { ArrayList(1) }).add(place)
        }

        fun addPlaces(name: SmartCastName, places: Collection<JetExpression>) {
            assert(!places.isEmpty())
            map.getOrPut(name, { ArrayList(places.size) }).addAll(places)
        }

        fun addIfCanBeSmartCast(expression: JetExpression) {
            val name = expression.smartCastExpressionName() ?: return
            if (filter(name)) {
                addPlace(name, expression)
            }
        }

        statement.accept(object : ControlFlowVisitor() {
            override fun visitPostfixExpression(expression: JetPostfixExpression) {
                expression.acceptChildren(this)

                if (expression.getOperationToken() == JetTokens.EXCLEXCL) {
                    addIfCanBeSmartCast(expression.getBaseExpression())
                }
            }

            override fun visitBinaryWithTypeRHSExpression(expression: JetBinaryExpressionWithTypeRHS) {
                expression.acceptChildren(this)

                if (expression.getOperationReference()?.getReferencedNameElementType() == JetTokens.AS_KEYWORD) {
                    addIfCanBeSmartCast(expression.getLeft())
                }
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                val condition = expression.getCondition()
                val thenBranch = expression.getThen()
                val elseBranch = expression.getElse()

                val smartCastNames = collectPossiblySmartCastInCondition(condition).filter(filter)
                if (smartCastNames.isNotEmpty()) {
                    val exits = collectAlwaysExitPoints(thenBranch) + collectAlwaysExitPoints(elseBranch)
                    if (exits.isNotEmpty()) {
                        for (name in smartCastNames) {
                            addPlaces(name, exits)
                        }
                    }
                }

                condition.accept(this)

                if (thenBranch != null && elseBranch != null) {
                    val thenCasts = potentialSmartCastPlaces(thenBranch, filter)
                    if (!thenCasts.isEmpty()) {
                        val elseCasts = potentialSmartCastPlaces(elseBranch) { filter(it) && thenCasts.containsKey(it) }
                        if (!elseCasts.isEmpty()) {
                            for ((name, places) in thenCasts) {
                                if (elseCasts.containsKey(name)) { // need filtering by cast names in else-branch
                                    addPlaces(name, places)
                                }
                            }

                            for ((name, places) in elseCasts) { // already filtered by cast names in then-branch
                                addPlaces(name, places)
                            }
                        }
                    }
                }
            }

            override fun visitForExpression(expression: JetForExpression) {
                // analyze only the loop-range expression, do not enter the loop body
                expression.getLoopRange()?.accept(this)
            }

            override fun visitWhileExpression(expression: JetWhileExpression) {
                val condition = expression.getCondition()
                // we need to enter the body only for "while(true)"
                if (condition.isTrueConstant()) {
                    expression.acceptChildren(this)
                }
                else {
                    condition?.accept(this)
                }
            }

            //TODO: when
        })

        return map
    }

    /**
     * Returns names of expressions that would possibly be smart cast after
     * either a statement "if (condition) return" or "if (!condition) return"
     */
    private fun collectPossiblySmartCastInCondition(condition: JetExpression?): Set<SmartCastName> {
        val result = HashSet<SmartCastName>()
        condition?.accept(object : ControlFlowVisitor() {
            override fun visitBinaryExpression(expression: JetBinaryExpression) {
                expression.acceptChildren(this)

                val operation = expression.getOperationToken()
                if (operation == JetTokens.EQEQ || operation == JetTokens.EXCLEQ || operation == JetTokens.EQEQEQ || operation == JetTokens.EXCLEQEQEQ) {
                    result.addIfNotNull(expression.getLeft()?.smartCastExpressionName())
                    result.addIfNotNull(expression.getRight()?.smartCastExpressionName())
                }
            }

            override fun visitIsExpression(expression: JetIsExpression) {
                expression.acceptChildren(this)

                result.addIfNotNull(expression.getLeftHandSide()?.smartCastExpressionName())
            }
        })
        return result
    }

    /**
     * If it's possible that the given statement never passes the execution to the next statement (that is, always exits somewhere)
     * then this function returns a collection of all places in code that are necessary to be kept to preserve this behaviour.
     */
    private fun collectAlwaysExitPoints(statement: JetExpression?): Collection<JetExpression> {
        val result = ArrayList<JetExpression>()
        statement?.accept(object : ControlFlowVisitor() {
            var insideLoop = false

            override fun visitReturnExpression(expression: JetReturnExpression) {
                result.add(expression)
            }

            override fun visitThrowExpression(expression: JetThrowExpression) {
                result.add(expression)
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                expression.getCondition().accept(this)

                val thenBranch = expression.getThen()
                val elseBranch = expression.getElse()
                if (thenBranch != null && elseBranch != null) { // if we have only one branch it makes no sense to search exits in it
                    val thenExits = collectAlwaysExitPoints(thenBranch)
                    if (thenExits.isNotEmpty()) {
                        val elseExits = collectAlwaysExitPoints(elseBranch)
                        if (elseExits.isNotEmpty()) {
                            result.addAll(thenExits)
                            result.addAll(elseExits)
                        }
                    }
                }
            }

            override fun visitForExpression(loop: JetForExpression) {
                loop.getLoopRange()?.accept(this)
                // do not make sense to search exits inside for as not necessary enter it at all
            }

            override fun visitWhileExpression(loop: JetWhileExpression) {
                val condition = loop.getCondition()
                if (condition.isTrueConstant()) {
                    insideLoop = true
                    loop.getBody()?.accept(this)
                    insideLoop = false
                }
                else {
                    // do not make sense to search exits inside while-loop as not necessary enter it at all
                    condition?.accept(this)
                }
            }

            override fun visitDoWhileExpression(loop: JetDoWhileExpression) {
                loop.getCondition()?.accept(this)
                insideLoop = true
                loop.getBody()?.accept(this)
                insideLoop = false
            }

            override fun visitBreakExpression(expression: JetBreakExpression) {
                if (!insideLoop || expression.getLabelName() != null) {
                    result.add(expression)
                }
            }

            override fun visitContinueExpression(expression: JetContinueExpression) {
                if (!insideLoop || expression.getLabelName() != null) {
                    result.add(expression)
                }
            }

            override fun visitCallExpression(expression: JetCallExpression) {
                val name = (expression.getCalleeExpression() as? JetSimpleNameExpression)?.getReferencedName()
                if (name != null && name in nothingFunctionNames) {
                    result.add(expression)
                }
                super.visitCallExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val name = expression.getReferencedName()
                if (name in nothingPropertyNames) {
                    result.add(expression)
                }
            }

            override fun visitBinaryExpression(expression: JetBinaryExpression) {
                if (expression.getOperationToken() == JetTokens.ELVIS) {
                    // do not search exits after "?:"
                    expression.getLeft()?.accept(this)
                }
                else {
                    super.visitBinaryExpression(expression)
                }
            }
        })
        return result
    }

    /**
     * Recursively visits code but does not enter constructs that may not affect smart casts/control flow
     */
    private abstract class ControlFlowVisitor : JetVisitorVoid() {
        override fun visitJetElement(element: JetElement) {
            if (element.noControlFlowInside()) return
            element.acceptChildren(this)
        }

        private fun JetElement.noControlFlowInside() = this is JetFunction || this is JetClass || this is JetClassBody
    }

    private fun PsiElement.isStatement() = this is JetExpression && getParent() is JetBlockExpression

    private data class SmartCastName(val receiverName: SmartCastName?, val selectorName: String) {
        override fun toString(): String = if (receiverName != null) receiverName.toString() + "." + selectorName else selectorName

        fun identifiers(): Collection<String> {
            return if (receiverName != null)
                receiverName.identifiers() + listOf(selectorName)
            else
                listOf(selectorName)
        }
    }

    //TODO: this can be smart-cast too!
    private fun JetExpression.smartCastExpressionName(): SmartCastName? {
        return when (this) {
            is JetSimpleNameExpression -> SmartCastName(null, this.getReferencedName())

            is JetQualifiedExpression -> {
                val selector = getSelectorExpression() as? JetSimpleNameExpression ?: return null
                val selectorName = selector.getReferencedName()
                val receiver = getReceiverExpression()
                if (receiver is JetThisExpression) {
                    return SmartCastName(null, selectorName)
                }
                val receiverName = receiver.smartCastExpressionName() ?: return null
                return SmartCastName(receiverName, selectorName)
            }

            else -> null
        }
    }

//    private fun JetExpression?.isNullLiteral() = this?.getNode()?.getElementType() == JetNodeTypes.NULL

    private fun JetExpression?.isTrueConstant()
            = this != null && getNode()?.getElementType() == JetNodeTypes.BOOLEAN_CONSTANT && getText() == "true"

    //TODO: review logic
    private fun isValueNeeded(expression: JetExpression): Boolean {
        val parent = expression.getParent()
        return when (parent) {
            is JetBlockExpression -> expression == parent.lastStatement() && isValueNeeded(parent)

            is JetContainerNode -> { //TODO - not quite correct
                val pparent = parent.getParent() as? JetExpression
                pparent != null && isValueNeeded(pparent)
            }

            is JetDeclarationWithBody -> {
                if (expression == parent.getBodyExpression())
                    !parent.hasBlockBody() && !parent.hasDeclaredReturnType()
                else
                    true
            }

            else -> true
        }
    }

    private fun JetBlockExpression.lastStatement(): JetExpression?
            = getLastChild()?.siblings(forward = false)?.firstOrNullIsInstance<JetExpression>()

    //TODO: declarations with special names (e.g. "get")
    private class NameFilter {
        private var names: MutableSet<String>? = HashSet()

        fun accepts(name: String) = names == null || name in names!!

        val isEmpty: Boolean
            get() = names?.isEmpty() ?: false

        private val addUsedNamesVisitor = object : JetVisitorVoid(){
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                names!!.add(expression.getReferencedName())
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }

            override fun visitBlockExpression(expression: JetBlockExpression) {
            }
        }

        fun addUsedNames(statement: JetExpression) {
            if (names != null) {
                statement.accept(addUsedNamesVisitor)
            }
        }

        fun addNames(filter: NameFilter) {
            if (names == null) return
            if (filter.names == null) {
                names = null
            }
            else {
                names!!.addAll(filter.names!!)
            }
        }

        fun addAllNames() {
            names = null
        }
    }

    private enum class MarkLevel {
        NONE
        RESOLVE_STATEMENT
        NEED_REFERENCE_RESOLVE
        NEED_COMPLETION
    }

    private inner class StatementTree {
        private val statementMarks = HashMap<JetExpression, MarkLevel>()
        private val blockLevels = HashMap<JetBlockExpression, MarkLevel>()

        val topLevelBlocks: Collection<JetBlockExpression> = blocks(body)

        fun blocks(statement: JetExpression): Collection<JetBlockExpression>
                = blocks(statement : JetElement)

        private fun blocks(element: JetElement): Collection<JetBlockExpression> {
            val result = ArrayList<JetBlockExpression>(1)
            element.accept(object : JetVisitorVoid() {
                override fun visitBlockExpression(expression: JetBlockExpression) {
                    result.add(expression)
                }

                override fun visitElement(element: PsiElement) {
                    element.acceptChildren(this)
                }
            })
            return result
        }

        fun mark(element: PsiElement, level: MarkLevel) {
            var e = element
            while (e != body) {
                if (e.isStatement()) {
                    markStatement(e as JetExpression, level)
                }
                e = e.getParent()!!
            }
        }

        private fun markStatement(statement: JetExpression, level: MarkLevel) {
            val currentLevel = statementMark(statement)
            if (currentLevel < level) {
                statementMarks[statement] = level

                val block = statement.getParent() as JetBlockExpression
                val currentBlockLevel = blockLevels[block] ?: MarkLevel.NONE
                if (currentBlockLevel < level) {
                    blockLevels[block] = level
                }
            }
        }

        fun statementMark(statement: JetExpression): MarkLevel
                = statementMarks[statement] ?: MarkLevel.NONE

        fun hasMarks(block: JetBlockExpression, minLevel: MarkLevel): Boolean {
            val level = blockLevels[block] ?: return minLevel == MarkLevel.NONE
            return level >= minLevel
        }
    }
}


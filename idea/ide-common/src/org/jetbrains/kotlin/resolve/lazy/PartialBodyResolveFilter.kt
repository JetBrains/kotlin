/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy

import java.util.HashSet
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.ArrayList
import java.util.HashMap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.utils.addToStdlib.swap
import org.jetbrains.kotlin.util.isProbablyNothing

//TODO: do resolve anonymous object's body

class PartialBodyResolveFilter(
        elementToResolve: JetElement,
        private val declaration: JetDeclaration,
        probablyNothingCallableNames: ProbablyNothingCallableNames,
        forCompletion: Boolean
) : StatementFilter() {

    private val statementMarks = StatementMarks()

    private val nothingFunctionNames = HashSet(probablyNothingCallableNames.functionNames())
    private val nothingVariableNames = HashSet(probablyNothingCallableNames.propertyNames())

    override val filter: ((JetElement) -> Boolean)? = { it is JetExpression && statementMarks.statementMark(it) != MarkLevel.SKIP }

    init {
        assert(declaration.isAncestor(elementToResolve))
        assert(!JetPsiUtil.isLocal(declaration),
               "Should never be invoked on local declaration otherwise we may miss some local declarations with type Nothing")

        declaration.accept(object : JetVisitorVoid() {
            override fun visitDeclaration(declaration: JetDeclaration) {
                super.visitDeclaration(declaration)

                if (declaration is JetCallableDeclaration) {
                    if (declaration.getTypeReference().containsProbablyNothing()) {
                        val name = declaration.getName()
                        if (name != null) {
                            if (declaration is JetNamedFunction) {
                                nothingFunctionNames.add(name)
                            }
                            else {
                                nothingVariableNames.add(name)
                            }
                        }
                    }
                }
            }

            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }
        })

        statementMarks.mark(elementToResolve, if (forCompletion) MarkLevel.NEED_COMPLETION else MarkLevel.NEED_REFERENCE_RESOLVE)
        declaration.blocks().forEach { processBlock(it) }
    }

    //TODO: do..while is special case

    private fun processBlock(block: JetBlockExpression): NameFilter {
        if (isValueNeeded(block)) {
            block.lastStatement()?.let { statementMarks.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
        }

        val nameFilter = NameFilter()
        val startStatement = statementMarks.lastMarkedStatement(block, MarkLevel.NEED_REFERENCE_RESOLVE) ?: return nameFilter

        for (statement in startStatement.siblings(forward = false)) {
            if (statement !is JetExpression) continue

            if (statement is JetNamedDeclaration) {
                val name = statement.getName()
                if (name != null && nameFilter(name)) {
                    statementMarks.mark(statement, MarkLevel.NEED_REFERENCE_RESOLVE)
                }
            }
            else if (statement is JetMultiDeclaration) {
                if (statement.getEntries().any {
                    val name = it.getName()
                    name != null && nameFilter(name)
                }) {
                    statementMarks.mark(statement, MarkLevel.NEED_REFERENCE_RESOLVE)
                }
            }

            fun updateNameFilter() {
                val level = statementMarks.statementMark(statement)
                when (level) {
                    MarkLevel.NEED_REFERENCE_RESOLVE -> nameFilter.addUsedNames(statement)
                    MarkLevel.NEED_COMPLETION -> nameFilter.addAllNames()
                }
            }

            updateNameFilter()

            if (!nameFilter.isEmpty) {
                val smartCastPlaces = potentialSmartCastPlaces(statement, { it.affectsNames(nameFilter) })
                if (!smartCastPlaces.isEmpty()) {
                    //TODO: do we really need correct resolve for ALL smart cast places?
                    smartCastPlaces.values()
                            .flatMap { it }
                            .forEach { statementMarks.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
                    updateNameFilter()
                }
            }

            val level = statementMarks.statementMark(statement)
            if (level > MarkLevel.TAKE) { // otherwise there are no statements inside that need processBlock which only works when reference resolve needed
                for (nestedBlock in statement.blocks()) {
                    val childFilter = processBlock(nestedBlock)
                    nameFilter.addNamesFromFilter(childFilter)
                }
            }
        }

        return nameFilter
    }

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
            if (places.isNotEmpty()) {
                map.getOrPut(name, { ArrayList(places.size()) }).addAll(places)
            }
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

                if (expression.getOperationReference().getReferencedNameElementType() == JetTokens.AS_KEYWORD) {
                    addIfCanBeSmartCast(expression.getLeft())
                }
            }

            override fun visitBinaryExpression(expression: JetBinaryExpression) {
                expression.acceptChildren(this)

                if (expression.getOperationToken() == JetTokens.ELVIS) {
                    val left = expression.getLeft()
                    val right = expression.getRight()
                    if (left != null && right != null) {
                        val smartCastName = left.smartCastExpressionName()
                        if (smartCastName != null && filter(smartCastName)) {
                            val exits = collectAlwaysExitPoints(right)
                            addPlaces(smartCastName, exits)
                        }
                    }
                }
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                val condition = expression.getCondition()
                val thenBranch = expression.getThen()
                val elseBranch = expression.getElse()

                val (thenSmartCastNames, elseSmartCastNames) = possiblySmartCastInCondition(condition)

                fun processBranchExits(smartCastNames: Collection<SmartCastName>, branch: JetExpression?) {
                    if (branch == null) return
                    val filteredNames = smartCastNames.filter(filter)
                    if (filteredNames.isNotEmpty()) {
                        val exits = collectAlwaysExitPoints(branch)
                        if (exits.isNotEmpty()) {
                            for (name in filteredNames) {
                                addPlaces(name, exits)
                            }
                        }
                    }
                }

                processBranchExits(thenSmartCastNames, elseBranch)
                processBranchExits(elseSmartCastNames, thenBranch)

                condition?.accept(this)

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
     * Returns names of expressions that would possibly be smart cast
     * in then (first component) and else (second component)
     * branches of an if-statement with such condition
     */
    private fun possiblySmartCastInCondition(condition: JetExpression?): Pair<Set<SmartCastName>, Set<SmartCastName>> {
        val emptyResult = Pair(setOf<SmartCastName>(), setOf<SmartCastName>())
        when (condition) {
            is JetBinaryExpression -> {
                val operation = condition.getOperationToken()
                val left = condition.getLeft() ?: return emptyResult
                val right = condition.getRight() ?: return emptyResult

                fun smartCastInEq(): Pair<Set<SmartCastName>, Set<SmartCastName>> {
                    if (left.isNullLiteral()) {
                        return Pair(setOf(), right.smartCastExpressionName().singletonOrEmptySet())
                    }
                    else if (right.isNullLiteral()) {
                        return Pair(setOf(), left.smartCastExpressionName().singletonOrEmptySet())
                    }
                    else {
                        val leftName = left.smartCastExpressionName()
                        val rightName = right.smartCastExpressionName()
                        val names = listOf(leftName, rightName).filterNotNull().toSet()
                        return Pair(names, setOf())
                    }
                }

                when (operation) {
                    JetTokens.EQEQ, JetTokens.EQEQEQ -> return smartCastInEq()

                    JetTokens.EXCLEQ, JetTokens.EXCLEQEQEQ -> return smartCastInEq().swap()

                    JetTokens.ANDAND -> {
                        val casts1 = possiblySmartCastInCondition(left)
                        val casts2 = possiblySmartCastInCondition(right)
                        return Pair(casts1.first.union(casts2.first), casts1.second.intersect(casts2.second))
                    }

                    JetTokens.OROR -> {
                        val casts1 = possiblySmartCastInCondition(left)
                        val casts2 = possiblySmartCastInCondition(right)
                        return Pair(casts1.first.intersect(casts2.first), casts1.second.union(casts2.second))
                    }
                }
            }

            is JetIsExpression -> {
                val cast = condition.getLeftHandSide().smartCastExpressionName().singletonOrEmptySet()
                return if (condition.isNegated()) Pair(setOf(), cast) else Pair(cast, setOf())
            }

            is JetPrefixExpression -> {
                if (condition.getOperationToken() == JetTokens.EXCL) {
                    val operand = condition.getBaseExpression() ?: return emptyResult
                    return possiblySmartCastInCondition(operand).swap()
                }
            }

            is JetParenthesizedExpression -> {
                val operand = condition.getExpression() ?: return emptyResult
                return possiblySmartCastInCondition(operand)
            }
        }

        return emptyResult
    }

    /**
     * If it's possible that the given statement never passes the execution to the next statement (that is, always exits somewhere)
     * then this function returns a collection of all places in code that are necessary to be kept to preserve this behaviour.
     */
    private fun collectAlwaysExitPoints(statement: JetExpression?): Collection<JetExpression> {
        val result = ArrayList<JetExpression>()
        statement?.accept(object : ControlFlowVisitor() {
            var insideLoopLevel: Int = 0

            override fun visitReturnExpression(expression: JetReturnExpression) {
                result.add(expression)
            }

            override fun visitThrowExpression(expression: JetThrowExpression) {
                result.add(expression)
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                expression.getCondition()?.accept(this)

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
                val condition = loop.getCondition() ?: return
                if (condition.isTrueConstant()) {
                    insideLoopLevel++
                    loop.getBody()?.accept(this)
                    insideLoopLevel--
                }
                else {
                    // do not make sense to search exits inside while-loop as not necessary enter it at all
                    condition.accept(this)
                }
            }

            override fun visitDoWhileExpression(loop: JetDoWhileExpression) {
                loop.getCondition()?.accept(this)
                insideLoopLevel++
                loop.getBody()?.accept(this)
                insideLoopLevel--
            }

            override fun visitBreakExpression(expression: JetBreakExpression) {
                if (insideLoopLevel == 0 || expression.getLabelName() != null) {
                    result.add(expression)
                }
            }

            override fun visitContinueExpression(expression: JetContinueExpression) {
                if (insideLoopLevel == 0 || expression.getLabelName() != null) {
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
                if (name in nothingVariableNames) {
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

    private data class SmartCastName(
            private val receiverName: SmartCastName?,
            private val selectorName: String? /* null means "this" (and receiverName should be null */
    ) {
        init {
            if (selectorName == null) {
                assert(receiverName == null, "selectorName is allowed to be null only when receiverName is also null (which means 'this')")
            }
        }

        override fun toString(): String = if (receiverName != null) receiverName.toString() + "." + selectorName else selectorName ?: "this"

        fun affectsNames(nameFilter: (String) -> Boolean): Boolean {
            if (selectorName == null) return true
            if (!nameFilter(selectorName)) return false
            return receiverName == null || receiverName.affectsNames(nameFilter)
        }
    }

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

            is JetThisExpression -> SmartCastName(null, null)

            else -> null
        }
    }

    //TODO: declarations with special names (e.g. "get")
    private class NameFilter : (String) -> Boolean {
        private var names: MutableSet<String>? = HashSet()

        override fun invoke(name: String) = names == null || name in names!!

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

        fun addNamesFromFilter(filter: NameFilter) {
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
        SKIP,
        TAKE,
        NEED_REFERENCE_RESOLVE,
        NEED_COMPLETION
    }

    companion object {
        private fun JetElement.blocks(): Collection<JetBlockExpression> {
            val result = ArrayList<JetBlockExpression>(1)
            this.accept(object : JetVisitorVoid() {
                override fun visitBlockExpression(expression: JetBlockExpression) {
                    result.add(expression)
                }

                override fun visitElement(element: PsiElement) {
                    element.acceptChildren(this)
                }
            })
            return result
        }

        private fun JetExpression?.isNullLiteral() = this?.getNode()?.getElementType() == JetNodeTypes.NULL

        private fun JetExpression?.isTrueConstant()
                = this != null && getNode()?.getElementType() == JetNodeTypes.BOOLEAN_CONSTANT && getText() == "true"

        private fun <T : Any> T?.singletonOrEmptySet(): Set<T> = if (this != null) setOf(this) else setOf()

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
                = getLastChild()?.siblings(forward = false)?.firstIsInstanceOrNull<JetExpression>()

        private fun PsiElement.isStatement() = this is JetExpression && getParent() is JetBlockExpression

        private fun JetTypeReference?.containsProbablyNothing(): Boolean {
            var result = false
            this?.getTypeElement()?.accept(object : JetVisitorVoid() {
                override fun visitJetElement(element: JetElement) {
                    element.acceptChildren(this)
                }

                override fun visitUserType(type: JetUserType) {
                    if (type.isProbablyNothing()) {
                        result = true
                    }
                }
            })
            return result
        }
    }

    private inner class StatementMarks {
        private val statementMarks = HashMap<JetExpression, MarkLevel>()
        private val blockLevels = HashMap<JetBlockExpression, MarkLevel>()

        fun mark(element: PsiElement, level: MarkLevel) {
            var e = element
            while (e != declaration) {
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
                val currentBlockLevel = blockLevels[block] ?: MarkLevel.SKIP
                if (currentBlockLevel < level) {
                    blockLevels[block] = level
                }
            }
        }

        fun statementMark(statement: JetExpression): MarkLevel
                = statementMarks[statement] ?: MarkLevel.SKIP

        fun lastMarkedStatement(block: JetBlockExpression, minLevel: MarkLevel): JetExpression? {
            val level = blockLevels[block] ?: MarkLevel.SKIP
            if (level < minLevel) return null // optimization
            return block.getLastChild().siblings(forward = false)
                    .filterIsInstance<JetExpression>()
                    .first { statementMark(it) >= minLevel }
        }
    }
}


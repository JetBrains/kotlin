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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.util.isProbablyNothing
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.swap
import java.util.*

//TODO: do resolve anonymous object's body

class PartialBodyResolveFilter(
        elementsToResolve: Collection<KtElement>,
        private val declaration: KtDeclaration,
        forCompletion: Boolean
) : StatementFilter() {

    private val statementMarks = StatementMarks()

    private val globalProbablyNothingCallableNames = ProbablyNothingCallableNames.getInstance(declaration.project)
    private val globalProbablyContractedCallableNames = ProbablyContractedCallableNames.getInstance(declaration.project)

    private val contextNothingFunctionNames = HashSet<String>()
    private val contextNothingVariableNames = HashSet<String>()

    override val filter: ((KtExpression) -> Boolean)? = { statementMarks.statementMark(it) != MarkLevel.NONE }

    val allStatementsToResolve: Collection<KtExpression>
        get() = statementMarks.allMarkedStatements()

    init {
        elementsToResolve.forEach { assert(declaration.isAncestor(it)) }
        assert(!KtPsiUtil.isLocal(declaration)) { "Should never be invoked on local declaration otherwise we may miss some local declarations with type Nothing" }

        declaration.forEachDescendantOfType<KtCallableDeclaration> { declaration ->
            if (declaration.typeReference.containsProbablyNothing()) {
                val name = declaration.name
                if (name != null) {
                    if (declaration is KtNamedFunction) {
                        contextNothingFunctionNames.add(name)
                    }
                    else {
                        contextNothingVariableNames.add(name)
                    }
                }
            }
        }

        elementsToResolve.forEach {
            statementMarks.mark(it, if (forCompletion) MarkLevel.NEED_COMPLETION else MarkLevel.NEED_REFERENCE_RESOLVE)
        }
        declaration.forTopLevelBlocksInside { processBlock(it) }
    }

    //TODO: do..while is special case

    private fun processBlock(block: KtBlockExpression): NameFilter {
        if (isValueNeeded(block)) {
            block.lastStatement()?.let { statementMarks.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
        }

        val nameFilter = NameFilter()
        val startStatement = statementMarks.lastMarkedStatement(block, MarkLevel.NEED_REFERENCE_RESOLVE) ?: return nameFilter

        for (statement in startStatement.siblings(forward = false)) {
            if (statement !is KtExpression) continue

            if (statement is KtNamedDeclaration) {
                val name = statement.getName()
                if (name != null && nameFilter(name)) {
                    statementMarks.mark(statement, MarkLevel.NEED_REFERENCE_RESOLVE)
                }
            }
            else if (statement is KtDestructuringDeclaration) {
                if (statement.entries.any {
                    val name = it.name
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
                    smartCastPlaces.values
                            .flatten()
                            .forEach { statementMarks.mark(it, MarkLevel.NEED_REFERENCE_RESOLVE) }
                    updateNameFilter()
                }
            }

            val level = statementMarks.statementMark(statement)
            if (level > MarkLevel.TAKE) { // otherwise there are no statements inside that need processBlock which only works when reference resolve needed
                statement.forTopLevelBlocksInside { nestedBlock ->
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
            statement: KtExpression,
            filter: (SmartCastName) -> Boolean = { true }
    ): Map<SmartCastName, List<KtExpression>> {
        val map = HashMap<SmartCastName, ArrayList<KtExpression>>(0)

        fun addPlace(name: SmartCastName, place: KtExpression) {
            map.getOrPut(name, { ArrayList(1) }).add(place)
        }

        fun addPlaces(name: SmartCastName, places: Collection<KtExpression>) {
            if (places.isNotEmpty()) {
                map.getOrPut(name, { ArrayList(places.size) }).addAll(places)
            }
        }

        fun addIfCanBeSmartCast(expression: KtExpression) {
            val name = expression.smartCastExpressionName() ?: return
            if (filter(name)) {
                addPlace(name, expression)
            }
        }

        statement.accept(object : ControlFlowVisitor() {
            override fun visitPostfixExpression(expression: KtPostfixExpression) {
                expression.acceptChildren(this)

                if (expression.operationToken == KtTokens.EXCLEXCL) {
                    addIfCanBeSmartCast(expression.baseExpression ?: return)
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                val nameReference = expression.calleeExpression as? KtNameReferenceExpression ?: return
                if (!globalProbablyContractedCallableNames.isProbablyContractedCallableName(nameReference.getReferencedName())) return

                val mentionedSmartCastName = expression.findMentionedName(filter)

                if (mentionedSmartCastName != null) {
                    addPlace(mentionedSmartCastName, expression)
                }
            }

            override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
                expression.acceptChildren(this)

                if (expression.operationReference.getReferencedNameElementType() == KtTokens.AS_KEYWORD) {
                    addIfCanBeSmartCast(expression.left)
                }
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                expression.acceptChildren(this)

                if (expression.operationToken == KtTokens.ELVIS) {
                    val left = expression.left
                    val right = expression.right
                    if (left != null && right != null) {
                        val smartCastName = left.smartCastExpressionName()
                        if (smartCastName != null && filter(smartCastName)) {
                            val exits = collectAlwaysExitPoints(right)
                            addPlaces(smartCastName, exits)
                        }
                    }
                }
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                val condition = expression.condition
                val thenBranch = expression.then
                val elseBranch = expression.`else`

                val (thenSmartCastNames, elseSmartCastNames) = possiblySmartCastInCondition(condition)

                fun processBranchExits(smartCastNames: Collection<SmartCastName>, branch: KtExpression?) {
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

            override fun visitForExpression(expression: KtForExpression) {
                // analyze only the loop-range expression, do not enter the loop body
                expression.loopRange?.accept(this)
            }

            override fun visitWhileExpression(expression: KtWhileExpression) {
                val condition = expression.condition
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

    private fun KtExpression.findMentionedName(filter: (SmartCastName) -> Boolean): SmartCastName? {
        var foundMentionedName: SmartCastName? = null

        val visitor = object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (foundMentionedName != null) return

                if (element !is KtSimpleNameExpression) super.visitElement(element)
                if (element !is KtExpression) return

                element.smartCastExpressionName()?.takeIf(filter)?.let { foundMentionedName = it }
            }
        }

        accept(visitor)

        return foundMentionedName
    }

    /**
     * Returns names of expressions that would possibly be smart cast
     * in then (first component) and else (second component)
     * branches of an if-statement with such condition
     */
    private fun possiblySmartCastInCondition(condition: KtExpression?): Pair<Set<SmartCastName>, Set<SmartCastName>> {
        val emptyResult = Pair(setOf<SmartCastName>(), setOf<SmartCastName>())
        when (condition) {
            is KtBinaryExpression -> {
                val operation = condition.operationToken
                val left = condition.left ?: return emptyResult
                val right = condition.right ?: return emptyResult

                fun smartCastInEq(): Pair<Set<SmartCastName>, Set<SmartCastName>> = when {
                    left.isNullLiteral() -> {
                        Pair(setOf(), right.smartCastExpressionName().singletonOrEmptySet())
                    }
                    right.isNullLiteral() -> {
                        Pair(setOf(), left.smartCastExpressionName().singletonOrEmptySet())
                    }
                    else -> {
                        val leftName = left.smartCastExpressionName()
                        val rightName = right.smartCastExpressionName()
                        val names = listOfNotNull(leftName, rightName).toSet()
                        Pair(names, setOf())
                    }
                }

                when (operation) {
                    KtTokens.EQEQ, KtTokens.EQEQEQ -> return smartCastInEq()

                    KtTokens.EXCLEQ, KtTokens.EXCLEQEQEQ -> return smartCastInEq().swap()

                    KtTokens.ANDAND -> {
                        val casts1 = possiblySmartCastInCondition(left)
                        val casts2 = possiblySmartCastInCondition(right)
                        return Pair(casts1.first.union(casts2.first), casts1.second.intersect(casts2.second))
                    }

                    KtTokens.OROR -> {
                        val casts1 = possiblySmartCastInCondition(left)
                        val casts2 = possiblySmartCastInCondition(right)
                        return Pair(casts1.first.intersect(casts2.first), casts1.second.union(casts2.second))
                    }
                }
            }

            is KtIsExpression -> {
                val cast = condition.leftHandSide.smartCastExpressionName().singletonOrEmptySet()
                return if (condition.isNegated) Pair(setOf(), cast) else Pair(cast, setOf())
            }

            is KtPrefixExpression -> {
                if (condition.operationToken == KtTokens.EXCL) {
                    val operand = condition.baseExpression ?: return emptyResult
                    return possiblySmartCastInCondition(operand).swap()
                }
            }

            is KtParenthesizedExpression -> {
                val operand = condition.expression ?: return emptyResult
                return possiblySmartCastInCondition(operand)
            }
        }

        return emptyResult
    }

    /**
     * If it's possible that the given statement never passes the execution to the next statement (that is, always exits somewhere)
     * then this function returns a collection of all places in code that are necessary to be kept to preserve this behaviour.
     */
    private fun collectAlwaysExitPoints(statement: KtExpression?): Collection<KtExpression> {
        val result = ArrayList<KtExpression>()
        statement?.accept(object : ControlFlowVisitor() {
            var insideLoopLevel: Int = 0

            override fun visitReturnExpression(expression: KtReturnExpression) {
                result.add(expression)
            }

            override fun visitThrowExpression(expression: KtThrowExpression) {
                result.add(expression)
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                expression.condition?.accept(this)

                val thenBranch = expression.then
                val elseBranch = expression.`else`
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

            override fun visitForExpression(loop: KtForExpression) {
                loop.loopRange?.accept(this)
                // do not make sense to search exits inside for as not necessary enter it at all
            }

            override fun visitWhileExpression(loop: KtWhileExpression) {
                val condition = loop.condition ?: return
                if (condition.isTrueConstant()) {
                    insideLoopLevel++
                    loop.body?.accept(this)
                    insideLoopLevel--
                }
                else {
                    // do not make sense to search exits inside while-loop as not necessary enter it at all
                    condition.accept(this)
                }
            }

            override fun visitDoWhileExpression(loop: KtDoWhileExpression) {
                loop.condition?.accept(this)
                insideLoopLevel++
                loop.body?.accept(this)
                insideLoopLevel--
            }

            override fun visitBreakExpression(expression: KtBreakExpression) {
                if (insideLoopLevel == 0 || expression.getLabelName() != null) {
                    result.add(expression)
                }
            }

            override fun visitContinueExpression(expression: KtContinueExpression) {
                if (insideLoopLevel == 0 || expression.getLabelName() != null) {
                    result.add(expression)
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val name = (expression.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                if (name != null && (name in globalProbablyNothingCallableNames.functionNames() || name in contextNothingFunctionNames)) {
                    result.add(expression)
                }
                super.visitCallExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val name = expression.getReferencedName()
                if (name in globalProbablyNothingCallableNames.propertyNames() || name in contextNothingVariableNames) {
                    result.add(expression)
                }
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (expression.operationToken == KtTokens.ELVIS) {
                    // do not search exits after "?:"
                    expression.left?.accept(this)
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
    private abstract class ControlFlowVisitor : KtVisitorVoid() {
        override fun visitKtElement(element: KtElement) {
            if (element.noControlFlowInside()) return
            element.acceptChildren(this)
        }

        private fun KtElement.noControlFlowInside() = this is KtFunction || this is KtClass || this is KtClassBody
    }

    private data class SmartCastName(
            private val receiverName: SmartCastName?,
            private val selectorName: String? /* null means "this" (and receiverName should be null */
    ) {
        init {
            if (selectorName == null) {
                assert(receiverName == null) { "selectorName is allowed to be null only when receiverName is also null (which means 'this')" }
            }
        }

        override fun toString(): String = if (receiverName != null) receiverName.toString() + "." + selectorName else selectorName ?: "this"

        fun affectsNames(nameFilter: (String) -> Boolean): Boolean {
            if (selectorName == null) return true
            if (!nameFilter(selectorName)) return false
            return receiverName == null || receiverName.affectsNames(nameFilter)
        }
    }

    private fun KtExpression.smartCastExpressionName(): SmartCastName? {
        return when (this) {
            is KtSimpleNameExpression -> SmartCastName(null, this.getReferencedName())

            is KtQualifiedExpression -> {
                val selector = selectorExpression as? KtSimpleNameExpression ?: return null
                val selectorName = selector.getReferencedName()
                val receiver = receiverExpression
                if (receiver is KtThisExpression) {
                    return SmartCastName(null, selectorName)
                }
                val receiverName = receiver.smartCastExpressionName() ?: return null
                return SmartCastName(receiverName, selectorName)
            }

            is KtThisExpression -> SmartCastName(null, null)

            else -> null
        }
    }

    //TODO: declarations with special names (e.g. "get")
    private class NameFilter : (String) -> Boolean {
        private var names: MutableSet<String>? = HashSet()

        override fun invoke(name: String) = names == null || name in names!!

        val isEmpty: Boolean
            get() = names?.isEmpty() ?: false

        fun addUsedNames(statement: KtExpression) {
            if (names != null) {
                statement.forEachDescendantOfType<KtSimpleNameExpression>(canGoInside = { it !is KtBlockExpression }) {
                    names!!.add(it.getReferencedName())
                }
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
        NONE,
        TAKE,
        NEED_REFERENCE_RESOLVE,
        NEED_COMPLETION
    }

    companion object {
        fun findStatementToResolve(element: KtElement, declaration: KtDeclaration): KtExpression? {
            return element.parentsWithSelf.takeWhile { it != declaration }.firstOrNull { it.isStatement() } as KtExpression?
        }

        private fun KtElement.forTopLevelBlocksInside(action: (KtBlockExpression) -> Unit) {
            forEachDescendantOfType(canGoInside = { it !is KtBlockExpression }, action = action)
        }

        private fun KtExpression?.isNullLiteral() = this?.node?.elementType == KtNodeTypes.NULL

        private fun KtExpression?.isTrueConstant()
                = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "true"

        private fun <T : Any> T?.singletonOrEmptySet(): Set<T> = if (this != null) setOf(this) else setOf()

        //TODO: review logic
        private fun isValueNeeded(expression: KtExpression): Boolean {
            val parent = expression.parent
            return when (parent) {
                is KtBlockExpression -> expression == parent.lastStatement() && isValueNeeded(parent)

                is KtContainerNode -> { //TODO - not quite correct
                    val pparent = parent.parent as? KtExpression
                    pparent != null && isValueNeeded(pparent)
                }

                is KtDeclarationWithBody -> {
                    if (expression == parent.bodyExpression)
                        !parent.hasBlockBody() && !parent.hasDeclaredReturnType()
                    else
                        true
                }

                is KtAnonymousInitializer -> false

                else -> true
            }
        }

        private fun KtBlockExpression.lastStatement(): KtExpression?
                = lastChild?.siblings(forward = false)?.firstIsInstanceOrNull<KtExpression>()

        private fun PsiElement.isStatement() = this is KtExpression && parent is KtBlockExpression

        private fun KtTypeReference?.containsProbablyNothing()
                = this?.typeElement?.anyDescendantOfType<KtUserType> { it.isProbablyNothing() } ?: false
    }

    private inner class StatementMarks {
        private val statementMarks = HashMap<KtExpression, MarkLevel>()
        private val blockLevels = HashMap<KtBlockExpression, MarkLevel>()

        fun mark(element: PsiElement, level: MarkLevel) {
            var e = element
            while (e != declaration) {
                if (e.isStatement()) {
                    markStatement(e as KtExpression, level)
                }
                e = e.parent!!
            }
        }

        private fun markStatement(statement: KtExpression, level: MarkLevel) {
            val currentLevel = statementMark(statement)
            if (currentLevel < level) {
                statementMarks[statement] = level

                val block = statement.parent as KtBlockExpression
                val currentBlockLevel = blockLevels[block] ?: MarkLevel.NONE
                if (currentBlockLevel < level) {
                    blockLevels[block] = level
                }
            }
        }

        fun statementMark(statement: KtExpression): MarkLevel
                = statementMarks[statement] ?: MarkLevel.NONE

        fun allMarkedStatements(): Collection<KtExpression>
                = statementMarks.keys

        fun lastMarkedStatement(block: KtBlockExpression, minLevel: MarkLevel): KtExpression? {
            val level = blockLevels[block] ?: MarkLevel.NONE
            if (level < minLevel) return null // optimization
            return block.lastChild.siblings(forward = false)
                    .filterIsInstance<KtExpression>()
                    .first { statementMark(it) >= minLevel }
        }
    }
}


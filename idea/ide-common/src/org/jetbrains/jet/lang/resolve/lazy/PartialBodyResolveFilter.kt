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

    private val statementsToResolve = HashSet<JetExpression>()
    private val processedBlocks = HashSet<JetBlockExpression>()

    private val nothingFunctionNames = HashSet(probablyNothingCallableNamesService.functionNames())
    private val nothingPropertyNames = probablyNothingCallableNamesService.propertyNames()

    ;{
        assert(body.isAncestor(elementToResolve, strict = false))

        body.accept(object : JetVisitorVoid(){
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

        addStatementsToResolve(elementToResolve)
    }

    override fun invoke(statement: JetElement): Boolean {
        val block = statement.getParent() as JetBlockExpression
        if (block !in processedBlocks) {
            processBlock(block)
        }
        return statement in statementsToResolve
    }

    tailRecursive
    private fun addStatementsToResolve(element: JetElement) {
        if (element == body) return
        val parent = element.getParent() as? JetElement ?: return

        if (parent is JetBlockExpression) {
            processBlock(parent)
            if (element in statementsToResolve) return // already processed

            if (element is JetExpression) {
                statementsToResolve.add(element)
            }

            for (statement in element.siblings(forward = false, withItself = false)) {
                if (statement !is JetExpression) continue
                if (statement is JetClassBody) continue

                val smartCastPlaces = potentialSmartCastPlaces(statement)
                if (!smartCastPlaces.isEmpty()) {
                    statementsToResolve.add(statement)
                    statementsToResolve.addStatementsForPlaces(smartCastPlaces.values().flatMap { it })
                }
                else if (statement is JetDeclaration) {
                    statementsToResolve.add(statement)
                }
            }
        }

        addStatementsToResolve(parent)
    }

    private fun processBlock(block: JetBlockExpression) {
        if (processedBlocks.add(block)) {
            val lastStatement = block.lastStatement()
            if (lastStatement != null && lastStatement !in statementsToResolve && isValueNeeded(block)) {
                addStatementsToResolve(lastStatement)
            }
        }
    }

    /**
     * Finds places within the given statement that may affect smart-casts after it.
     * That is, such places whose containing statements must be left in code to keep the smart-casts.
     * Returns map from smart-cast expression names (variable name or qualified variable name) to places.
     */
    private fun potentialSmartCastPlaces(statement: JetExpression, filter: (String) -> Boolean = { true }): Map<String, List<JetExpression>> {
        val map = HashMap<String, ArrayList<JetExpression>>(0)

        fun addPlace(name: String, place: JetExpression) {
            map.getOrPut(name, { ArrayList(1) }).add(place)
        }

        fun addPlaces(name: String, places: Collection<JetExpression>) {
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
    private fun collectPossiblySmartCastInCondition(condition: JetExpression?): Set<String> {
        val result = HashSet<String>()
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
                    // do not make sense to search exits inside for as not necessary enter it at all
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
    private inner abstract class ControlFlowVisitor : JetVisitorVoid() {
        override fun visitJetElement(element: JetElement) {
            if (element.noControlFlowInside()) return
            element.acceptChildren(this)
        }
    }

    private fun JetElement.noControlFlowInside() = this is JetFunction || this is JetClass || this is JetClassBody

    private fun MutableSet<JetExpression>.addStatementsForPlaces(places: Collection<JetExpression>) {
        for (place in places) {
            var parent: PsiElement = place
            while (true) {
                if (parent.isStatement() && !add(parent as JetExpression)) break
                parent = parent.getParent()
            }
        }
    }

    private fun PsiElement.isStatement() = this is JetExpression && getParent() is JetBlockExpression

    private fun JetExpression.smartCastExpressionName(): String? {
        return when (this) {
            is JetSimpleNameExpression -> this.getReferencedName()

            is JetQualifiedExpression -> {
                val selectorName = getSelectorExpression().smartCastExpressionName() ?: return null
                val receiver = getReceiverExpression()
                if (receiver is JetThisExpression) return selectorName
                val receiverName = receiver.smartCastExpressionName() ?: return null
                return selectorName + "." + receiverName
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
}


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

class PartialBodyResolveFilter(elementToResolve: JetElement, private val body: JetExpression) : (JetElement) -> Boolean {

    private val statementsToResolve = HashSet<JetExpression>()
    private val processedBlocks = HashSet<JetBlockExpression>()

    ;{
        addStatementsToResolve(elementToResolve)
    }

    override fun invoke(statement: JetElement): Boolean {
        val block = statement.getParent() as JetBlockExpression
        if (block !in processedBlocks) {
            processBlock(block)
        }
        return statement in statementsToResolve
    }

    private fun addStatementsToResolve(element: JetElement) {
        if (element == body) return
        val parent = element.getParent() as JetElement

        if (parent is JetBlockExpression) {
            processBlock(parent)
            if (element in statementsToResolve) return // already processed

            if (element is JetExpression) {
                statementsToResolve.add(element)
            }

            for (statement in element.siblings(forward = false, withItself = false)) {
                if (statement !is JetExpression) continue

                val smartCastPlaces = potentialSmartCastPlaces(statement)
                if (!smartCastPlaces.isEmpty()) {
                    statementsToResolve.add(statement)
                    statementsToResolve.addStatementsForPlaces(statement, smartCastPlaces.values().flatMap { it })
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

    private fun potentialSmartCastPlaces(expression: JetExpression, filter: (String) -> Boolean = { true }): Map<String, List<JetExpression>> {
        val map = HashMap<String, ArrayList<JetExpression>>(0)

        fun addPlace(name: String, place: JetExpression) {
            var list = map[name]
            if (list == null) {
                list = ArrayList(1)
                map[name] = list
            }
            list!!.add(place)
        }

        fun addPlaces(name: String, places: Collection<JetExpression>) {
            assert(!places.isEmpty())
            var list = map[name]
            if (list == null) {
                list = ArrayList(places.size)
                map[name] = list
            }
            list!!.addAll(places)
        }

        fun addIfCanBeSmartCasted(expression: JetExpression) {
            val name = expression.smartCastedExpressionName() ?: return
            if (filter(name)) {
                addPlace(name, expression)
            }
        }

        expression.accept(object : ControlFlowVisitor(){
            override fun visitPostfixExpression(expression: JetPostfixExpression) {
                expression.acceptChildren(this)

                if (expression.getOperationToken() == JetTokens.EXCLEXCL) {
                    addIfCanBeSmartCasted(expression.getBaseExpression())
                }
            }

            override fun visitBinaryWithTypeRHSExpression(expression: JetBinaryExpressionWithTypeRHS) {
                expression.acceptChildren(this)

                if (expression.getOperationReference()?.getReferencedNameElementType() == JetTokens.AS_KEYWORD) {
                    addIfCanBeSmartCasted(expression.getLeft())
                }
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                val condition = expression.getCondition()
                val thenBranch = expression.getThen()
                val elseBranch = expression.getElse()

                val smartCastedNames = collectPossiblySmartCastedInCondition(condition).filter(filter)
                if (smartCastedNames.isNotEmpty()) {
                    val exits = collectAlwaysExitPoints(thenBranch) + collectAlwaysExitPoints(elseBranch)
                    if (exits.isNotEmpty()) {
                        for (name in smartCastedNames) {
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
                //TODO: what about e.g. "1 == 1"
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

    private fun collectPossiblySmartCastedInCondition(condition: JetExpression?): Set<String> {
        val result = HashSet<String>()
        condition?.accept(object : ControlFlowVisitor() {
            override fun visitBinaryExpression(expression: JetBinaryExpression) {
                expression.acceptChildren(this)

                val operation = expression.getOperationToken()
                if (operation == JetTokens.EQEQ || operation == JetTokens.EXCLEQ) {
                    result.addIfNotNull(expression.getLeft()?.smartCastedExpressionName())
                    result.addIfNotNull(expression.getRight()?.smartCastedExpressionName())
                }
            }

            override fun visitIsExpression(expression: JetIsExpression) {
                expression.acceptChildren(this)

                result.addIfNotNull(expression.getLeftHandSide()?.smartCastedExpressionName())
            }
        })
        return result
    }

    //TODO: more precise analysis
    private fun collectAlwaysExitPoints(expression: JetExpression?): Collection<JetExpression> {
        val result = ArrayList<JetExpression>()
        expression?.accept(object : ControlFlowVisitor() {
            var insideLoop = false

            override fun visitReturnExpression(expression: JetReturnExpression) {
                result.add(expression)
            }

            override fun visitThrowExpression(expression: JetThrowExpression) {
                result.add(expression)
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
        })
        return result
    }

    private abstract class ControlFlowVisitor : JetVisitorVoid() {
        override fun visitJetElement(element: JetElement) {
            if (element.noControlFlowInside()) return
            element.acceptChildren(this)
        }

        private fun JetElement.noControlFlowInside() = this is JetFunction || this is JetClass || this is JetClassBody
    }

    private fun MutableSet<JetExpression>.addStatementsForPlaces(thisStatement: JetExpression, places: Collection<JetExpression>) {
        @PlacesLoop
        for (place in places) {
            var parent: PsiElement = place
            while (parent != thisStatement) {
                if (parent.isStatement()) {
                    if (!add(parent as JetExpression)) continue@PlacesLoop
                }
                parent = parent.getParent()
            }
        }
    }

    private fun PsiElement.isStatement() = this is JetExpression && getParent() is JetBlockExpression

    //TODO: this.a
    private fun JetExpression.smartCastedExpressionName(): String? {
        return when (this) {
            is JetSimpleNameExpression -> this.getReferencedName()

            is JetQualifiedExpression -> {
                val selectorName = getSelectorExpression().smartCastedExpressionName() ?: return null
                val receiverName = getReceiverExpression().smartCastedExpressionName() ?: return null
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
            = getLastChild().siblings(forward = false).filterIsInstance<JetExpression>().firstOrNull()
}


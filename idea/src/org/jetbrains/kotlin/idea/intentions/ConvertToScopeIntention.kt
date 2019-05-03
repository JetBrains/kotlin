/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.intentions.ConvertToScopeIntention.ScopeFunction.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

sealed class ConvertToScopeIntention(
    private val scopeFunction: ScopeFunction
) : SelfTargetingIntention<KtExpression>(KtExpression::class.java, "Convert to ${scopeFunction.functionName}") {

    enum class ScopeFunction(val functionName: String, val isParameterScope: Boolean) {
        ALSO(functionName = "also", isParameterScope = true),
        APPLY(functionName = "apply", isParameterScope = false),
        RUN(functionName = "run", isParameterScope = false),
        WITH(functionName = "with", isParameterScope = false);

        val receiver = if (isParameterScope) "it" else "this"
    }

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        when (element) {
            is KtProperty ->
                if (!element.isLocal) return false
            is KtCallExpression -> {
                if (element.parent is KtDotQualifiedExpression) return false
                val propertyName = element.prevProperty()?.name ?: return false
                if (!element.isTarget(propertyName)) return false
            }
            is KtDotQualifiedExpression -> {
                if (element.parent is KtDotQualifiedExpression) return false
                val name = element.getLeftMostReceiverExpression().text
                if (!element.isTarget(name)) return false
            }
            else ->
                return false
        }
        return element.collectTargetElements() != null
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val (targets, referenceName) = element.collectTargetElements() ?: return
        val first = targets.firstOrNull() ?: return
        val last = targets.lastOrNull() ?: return
        val property = element.prevProperty()
        val propertyOrFirst = when (scopeFunction) {
            ALSO, APPLY -> property
            else -> first
        } ?: return
        val parent = element.parent.let { if (it is KtBinaryExpression) it.parent else it }

        val psiFactory = KtPsiFactory(element)
        val (scopeFunctionCall, block) = psiFactory.createScopeFunctionCall(propertyOrFirst) ?: return
        block.addRange(property?.nextSibling ?: first, last)
        block.children.forEach { replace(it, referenceName, psiFactory) }
        parent.addBefore(scopeFunctionCall, propertyOrFirst)
        parent.deleteChildRange(propertyOrFirst, last)
    }

    private fun replace(element: PsiElement, referenceName: String, psiFactory: KtPsiFactory) {
        when (element) {
            is KtDotQualifiedExpression -> {
                val replaced = element.deleteFirstReceiver()
                if (scopeFunction.isParameterScope) {
                    replaced.replace(psiFactory.createExpressionByPattern("${scopeFunction.receiver}.$0", replaced))
                }
            }
            is KtCallExpression -> {
                element.valueArguments.forEach { arg ->
                    if (arg.getArgumentExpression()?.text == referenceName) {
                        arg.replace(psiFactory.createArgument(scopeFunction.receiver))
                    }
                }
            }
            is KtBinaryExpression -> {
                listOfNotNull(element.left, element.right).forEach {
                    replace(it, referenceName, psiFactory)
                }
            }
        }
    }

    private fun KtExpression.collectTargetElements(): Pair<List<PsiElement>, String>? {
        val (targets, referenceName) = when (scopeFunction) {
            ALSO, APPLY -> {
                val property = prevProperty() ?: return null
                val referenceName = property.name ?: return null
                val targets = property.collectTargetElements(referenceName, forward = true).toList()
                val parentOrThis = parent as? KtBinaryExpression ?: this
                if (this !is KtProperty && parentOrThis !in targets) return null
                targets to referenceName
            }
            else -> {
                if (this !is KtDotQualifiedExpression) return null
                val referenceName = getLeftMostReceiverExpression().text
                val prev = collectTargetElements(referenceName, forward = false).toList().reversed()
                val next = collectTargetElements(referenceName, forward = true)
                (prev + listOf(this) + next) to referenceName
            }
        }
        if (targets.isEmpty()) return null
        return targets to referenceName
    }

    private fun KtExpression.collectTargetElements(referenceName: String, forward: Boolean): Sequence<PsiElement> {
        val parentOrThis = parent as? KtBinaryExpression ?: this
        return parentOrThis.siblings(forward, withItself = false)
            .filter { it !is PsiWhiteSpace && it !is PsiComment }
            .takeWhile { it.isTarget(referenceName) }
    }

    private fun PsiElement.isTarget(referenceName: String): Boolean {
        when (this) {
            is KtDotQualifiedExpression -> {
                val leftMostReceiver = getLeftMostReceiverExpression()
                if (leftMostReceiver.text != referenceName) return false
                if (leftMostReceiver.mainReference?.resolve() is PsiClass) return false
                val callExpr = callExpression ?: return false
                if (callExpr.lambdaArguments.isNotEmpty() || callExpr.valueArguments.any { it.text == scopeFunction.receiver }) return false
            }
            is KtCallExpression -> {
                val valueArguments = this.valueArguments
                if (valueArguments.none { it.getArgumentExpression()?.text == referenceName }) return false
                if (lambdaArguments.isNotEmpty() || valueArguments.any { it.text == scopeFunction.receiver }) return false
            }
            is KtBinaryExpression -> {
                val left = this.left ?: return false
                val right = this.right ?: return false
                if (left !is KtDotQualifiedExpression && left !is KtCallExpression
                    && right !is KtDotQualifiedExpression && right !is KtCallExpression
                ) return false
                if ((left is KtDotQualifiedExpression || left is KtCallExpression) && !left.isTarget(referenceName)) return false
                if ((right is KtDotQualifiedExpression || right is KtCallExpression) && !right.isTarget(referenceName)) return false
            }
            else ->
                return false
        }
        return !anyDescendantOfType<KtNameReferenceExpression> { it.text == scopeFunction.receiver }
    }

    private fun KtExpression.prevProperty(): KtProperty? {
        val parentOrThis = parent as? KtBinaryExpression ?: this
        return parentOrThis.siblings(forward = false, withItself = true).firstOrNull { it is KtProperty && it.isLocal } as? KtProperty
    }

    private fun KtPsiFactory.createScopeFunctionCall(element: PsiElement): Pair<KtExpression, KtBlockExpression>? {
        val scopeFunctionName = scopeFunction.functionName
        val (scopeFunctionCall, callExpression) = when (scopeFunction) {
            ALSO, APPLY -> {
                if (element !is KtProperty) return null
                val propertyName = element.name ?: return null
                val initializer = element.initializer ?: return null
                val property = createProperty(
                    name = propertyName,
                    type = element.typeReference?.text,
                    isVar = element.isVar,
                    initializer = "${initializer.text}.$scopeFunctionName {}"
                )
                val callExpression = (property.initializer as? KtDotQualifiedExpression)?.callExpression ?: return null
                property to callExpression
            }
            RUN -> {
                if (element !is KtDotQualifiedExpression) return null
                val scopeFunctionCall = createExpressionByPattern(
                    "$0.$scopeFunctionName {}",
                    element.getLeftMostReceiverExpression()
                ) as? KtQualifiedExpression ?: return null
                val callExpression = scopeFunctionCall.callExpression ?: return null
                scopeFunctionCall to callExpression
            }
            WITH -> {
                if (element !is KtDotQualifiedExpression) return null
                val scopeFunctionCall = createExpressionByPattern(
                    "$scopeFunctionName($0) {}",
                    element.getLeftMostReceiverExpression()
                ) as? KtCallExpression ?: return null
                scopeFunctionCall to scopeFunctionCall
            }
        }
        val body = callExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression ?: return null
        return scopeFunctionCall to body
    }
}

class ConvertToAlsoIntention : ConvertToScopeIntention(ALSO)

class ConvertToApplyIntention : ConvertToScopeIntention(APPLY)

class ConvertToRunIntention : ConvertToScopeIntention(RUN)

class ConvertToWithIntention : ConvertToScopeIntention(WITH)

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.idea.intentions.ConvertToScopeIntention.ScopeFunction.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings

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

    private data class RefactoringTargetAndItsValueExpression(
        val targetElement: PsiElement,
        val targetElementValue: PsiElement
    )

    private data class ScopedFunctionCallAndBlock(
        val scopeFunctionCall: KtExpression,
        val block: KtBlockExpression
    )

    override fun isApplicableTo(element: KtExpression, caretOffset: Int) = tryApplyTo(element, dryRun = true)

    override fun applyTo(element: KtExpression, editor: Editor?) {
        if (!tryApplyTo(element, dryRun = false)) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                RefactoringBundle.message("refactoring.is.not.supported.in.the.current.context", text)
            )
            CommonRefactoringUtil.showErrorHint(element.project, editor, message, text, null)
        }
    }

    private fun KtExpression.tryGetExpressionToApply(referenceName: String): KtExpression? {
        val childOfBlock = PsiTreeUtil.findFirstParent(this) {
            it.parent is KtBlockExpression
        } as? KtExpression ?: return null

        return if (childOfBlock is KtProperty || childOfBlock.isTarget(referenceName)) childOfBlock else null
    }

    private fun tryApplyTo(element: KtExpression, dryRun: Boolean): Boolean {

        val invalidElementToRefactoring = when (element) {
            is KtProperty -> !element.isLocal
            is KtCallExpression -> false
            is KtDotQualifiedExpression -> false
            else -> true
        }
        if (invalidElementToRefactoring) return false

        val (referenceElement, referenceName) = element.tryExtractReferenceName() ?: return false
        val expressionToApply = element.tryGetExpressionToApply(referenceName) ?: return false
        val (firstTarget, lastTarget) = expressionToApply.collectTargetElementsRange(referenceName, greedy = !dryRun) ?: return false

        val refactoringTarget = tryGetFirstElementToRefactoring(expressionToApply, firstTarget) ?: return false

        if (dryRun) return true

        val psiFactory = KtPsiFactory(expressionToApply)

        val (scopeFunctionCall, block) =
            createScopeFunctionCall(psiFactory, refactoringTarget.targetElement) ?: return false

        replaceReference(referenceElement, refactoringTarget.targetElementValue, lastTarget, psiFactory)

        block.addRange(refactoringTarget.targetElementValue, lastTarget)

        if (!scopeFunction.isParameterScope) {
            removeRedundantThisQualifiers(block)
        }

        with(firstTarget) {
            parent.addBefore(scopeFunctionCall, this)
            parent.deleteChildRange(this, lastTarget)
        }

        return true
    }

    private fun removeRedundantThisQualifiers(block: KtBlockExpression) {
        val thisDotSomethingExpressions = block.collectDescendantsOfType<KtDotQualifiedExpression> {
            it.receiverExpression is KtThisExpression && it.selectorExpression !== null
        }
        thisDotSomethingExpressions.forEach { thisDotSomethingExpression ->
            thisDotSomethingExpression.selectorExpression?.let { selector ->
                thisDotSomethingExpression.replace(selector)
            }
        }
    }

    private fun tryGetFirstElementToRefactoring(expressionToApply: KtExpression, firstTarget: PsiElement)
            : RefactoringTargetAndItsValueExpression? {

        val property by lazy(LazyThreadSafetyMode.NONE) { expressionToApply.prevProperty() }

        val propertyOrFirst = when (scopeFunction) {
            ALSO, APPLY -> property
            RUN, WITH -> firstTarget
        } ?: return null

        val isCorrectFirstOrProperty = when (scopeFunction) {
            ALSO, APPLY -> propertyOrFirst is KtProperty && propertyOrFirst.name !== null && propertyOrFirst.initializer !== null
            RUN -> propertyOrFirst is KtDotQualifiedExpression
            WITH -> propertyOrFirst is KtDotQualifiedExpression
        }

        if (!isCorrectFirstOrProperty) return null

        return RefactoringTargetAndItsValueExpression(propertyOrFirst, property?.nextSibling ?: firstTarget)
    }

    private fun replaceReference(element: PsiElement, firstTarget: PsiElement, lastTarget: PsiElement, psiFactory: KtPsiFactory) {

        val replacement by lazy(LazyThreadSafetyMode.NONE) {
            if (scopeFunction.isParameterScope)
                psiFactory.createSimpleName(scopeFunction.receiver)
            else
                psiFactory.createThisExpression()
        }

        val searchParameters = KotlinReferencesSearchParameters(
            element, element.useScope, ignoreAccessScope = false
        )

        val range = PsiTreeUtil.getElementsOfRange(firstTarget, lastTarget)

        ReferencesSearch.search(searchParameters)
            .mapNotNull { it.element as? KtNameReferenceExpression }
            .filter { reference ->
                range.any { rangeElement -> PsiTreeUtil.isAncestor(rangeElement, reference, /* strict = */ true) }
            }
            .forEach { referenceInRange ->
                referenceInRange.replace(replacement)
            }
    }

    private fun KtExpression.tryExtractReferenceName(): Pair<PsiElement, String>? {
        return when (scopeFunction) {
            ALSO, APPLY -> {
                val property = prevProperty()
                val name = property?.name
                if (name !== null) property to name else null
            }
            RUN, WITH -> {
                val receiver = (this as? KtDotQualifiedExpression)
                    ?.let { getLeftMostReceiverExpression() } as? KtNameReferenceExpression

                val declaration = receiver?.mainReference?.resolve()
                val name = receiver?.getReferencedName()

                if (declaration !== null && name !== null) declaration to name else null
            }
        }
    }

    private fun KtExpression.collectTargetElementsRange(referenceName: String, greedy: Boolean): Pair<PsiElement, PsiElement>? {
        return when (scopeFunction) {
            ALSO, APPLY -> {
                val firstTarget = this as? KtProperty ?: this.prevProperty() ?: this

                val lastTargetSequence = firstTarget.collectTargetElements(referenceName, forward = true)

                val lastTarget = if (firstTarget === this)
                    if (greedy) lastTargetSequence.lastOrNull()
                    else lastTargetSequence.firstOrNull()
                else
                    if (greedy) lastTargetSequence.lastWithPersistedElementOrNull(elementShouldPersist = this)
                    else lastTargetSequence.firstOrNull { this === it }

                if (lastTarget !== null) firstTarget to lastTarget else null
            }
            RUN, WITH -> {

                val firstTarget = collectTargetElements(referenceName, forward = false).lastOrNull() ?: this

                val lastTarget =
                    if (greedy) collectTargetElements(referenceName, forward = true).lastOrNull() ?: this
                    else this

                firstTarget to lastTarget
            }
        }
    }

    private fun KtExpression.collectTargetElements(referenceName: String, forward: Boolean): Sequence<PsiElement> {
        return siblings(forward, withItself = false)
            .filter { it !is PsiWhiteSpace && it !is PsiComment && !(it is LeafPsiElement && it.elementType == KtTokens.SEMICOLON) }
            .takeWhile { it.isTarget(referenceName) }
    }

    private fun PsiElement.isTarget(referenceName: String): Boolean {
        when (this) {
            is KtDotQualifiedExpression -> {
                val callExpr = callExpression ?: return false
                if (callExpr.lambdaArguments.isNotEmpty() ||
                    callExpr.valueArguments.any { it.text == scopeFunction.receiver }
                ) return false

                val leftMostReceiver = getLeftMostReceiverExpression()
                if (leftMostReceiver.text != referenceName) return false

                if (leftMostReceiver.mainReference?.resolve() is PsiClass) return false
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
            else -> return false
        }
        return !anyDescendantOfType<KtNameReferenceExpression> { it.text == scopeFunction.receiver }
    }

    private fun KtExpression.prevProperty(): KtProperty? {
        val blockChildExpression = PsiTreeUtil.findFirstParent(this) {
            it.parent is KtBlockExpression
        } ?: return null

        return blockChildExpression
            .siblings(forward = false, withItself = true)
            .firstOrNull { it is KtProperty && it.isLocal } as? KtProperty
    }

    private fun createScopeFunctionCall(factory: KtPsiFactory, element: PsiElement): ScopedFunctionCallAndBlock? {
        val scopeFunctionName = scopeFunction.functionName
        val (scopeFunctionCall, callExpression) = when (scopeFunction) {
            ALSO, APPLY -> {
                if (element !is KtProperty) return null
                val propertyName = element.name ?: return null
                val initializer = element.initializer ?: return null

                val initializerPattern = when (initializer) {
                    is KtDotQualifiedExpression, is KtCallExpression, is KtConstantExpression, is KtParenthesizedExpression -> initializer.text
                    else -> "(${initializer.text})"
                }

                val property = factory.createProperty(
                    name = propertyName,
                    type = element.typeReference?.text,
                    isVar = element.isVar,
                    initializer = "$initializerPattern.$scopeFunctionName {}"
                )
                val callExpression = (property.initializer as? KtDotQualifiedExpression)?.callExpression ?: return null
                property to callExpression
            }
            RUN -> {
                if (element !is KtDotQualifiedExpression) return null
                val scopeFunctionCall = factory.createExpressionByPattern(
                    "$0.$scopeFunctionName {}",
                    element.getLeftMostReceiverExpression()
                ) as? KtQualifiedExpression ?: return null
                val callExpression = scopeFunctionCall.callExpression ?: return null
                scopeFunctionCall to callExpression
            }
            WITH -> {
                if (element !is KtDotQualifiedExpression) return null

                val scopeFunctionCall = factory.createExpressionByPattern(
                    "$scopeFunctionName($0) {}",
                    element.getLeftMostReceiverExpression()
                ) as? KtCallExpression ?: return null
                scopeFunctionCall to scopeFunctionCall
            }
        }

        val body = callExpression.lambdaArguments
            .firstOrNull()
            ?.getLambdaExpression()
            ?.bodyExpression
            ?: return null

        return ScopedFunctionCallAndBlock(scopeFunctionCall, body)
    }
}

class ConvertToAlsoIntention : ConvertToScopeIntention(ALSO)

class ConvertToApplyIntention : ConvertToScopeIntention(APPLY)

class ConvertToRunIntention : ConvertToScopeIntention(RUN)

class ConvertToWithIntention : ConvertToScopeIntention(WITH)

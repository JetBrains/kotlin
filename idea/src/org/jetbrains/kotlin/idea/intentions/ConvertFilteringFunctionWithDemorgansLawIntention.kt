/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.ReplaceNegatedIsEmptyWithIsNotEmptyInspection.Companion.invertSelectorFunction
import org.jetbrains.kotlin.idea.inspections.SimplifyNegatedBinaryExpressionInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class ConvertFilteringFunctionWithDemorgansLawIntention(
    private val fromFunctionName: String,
    private val toFunctionName: String,
    private val negateCall: Boolean,
    private val negatePredicate: Boolean
) : SelfTargetingRangeIntention<KtCallExpression>(
    KtCallExpression::class.java,
    KotlinBundle.lazyMessage("replace.0.with.1", fromFunctionName, toFunctionName)
) {
    override fun applicabilityRange(element: KtCallExpression): TextRange? {
        val callee = element.calleeExpression ?: return null
        if (callee.text != fromFunctionName) return null
        val fqNames = functions[fromFunctionName] ?: return null
        if (element.getQualifiedExpressionForSelector()?.getStrictParentOfType<KtDotQualifiedExpression>() != null) return null
        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (element.getResolvedCall(context)?.resultingDescriptor?.fqNameOrNull() !in fqNames) return null

        val lambda = element.lambda() ?: return null
        val lambdaBody = lambda.bodyExpression ?: return null
        if (lambdaBody.anyDescendantOfType<KtReturnExpression>()) return null
        if (lambdaBody.statements.lastOrNull()?.getType(context)?.isBoolean() != true) return null

        return callee.textRange
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val lambda = element.lambda() ?: return
        val lastExpression = lambda.bodyExpression?.statements?.lastOrNull() ?: return
        val callOrQualified = element.getQualifiedExpressionForSelector() ?: element
        val psiFactory = KtPsiFactory(element)

        if (negatePredicate) {
            val exclPrefixExpression = lastExpression.asExclPrefixExpression()
            if (exclPrefixExpression == null) {
                val replaced = lastExpression.replaced(psiFactory.createExpressionByPattern("!($0)", lastExpression)) as KtPrefixExpression
                replaced.baseExpression.removeUnnecessaryParentheses()
                when (val baseExpression = replaced.baseExpression?.deparenthesize()) {
                    is KtBinaryExpression -> {
                        val operationToken = baseExpression.operationToken
                        if (operationToken == KtTokens.ANDAND || operationToken == KtTokens.OROR) {
                            ConvertBinaryExpressionWithDemorgansLawIntention.convertIfPossible(baseExpression)
                        } else {
                            SimplifyNegatedBinaryExpressionInspection.simplifyNegatedBinaryExpressionIfNeeded(replaced)
                        }
                    }
                    is KtQualifiedExpression -> {
                        baseExpression.invertSelectorFunction()?.let { replaced.replace(it) }
                    }
                }
            } else {
                val replaced = exclPrefixExpression.baseExpression?.let { lastExpression.replaced(it) }
                replaced.removeUnnecessaryParentheses()
            }
        }

        val parentExclPrefixExpression =
            callOrQualified.parents.dropWhile { it is KtParenthesizedExpression }.firstOrNull()?.asExclPrefixExpression()
        psiFactory.buildExpression {
            appendFixedText(if (negateCall && parentExclPrefixExpression == null) "!" else "")
            if (callOrQualified is KtQualifiedExpression) {
                appendExpression(callOrQualified.receiverExpression)
                appendFixedText(".")
            }
            appendFixedText(toFunctionName)
            element.valueArgumentList?.let {
                appendFixedText(it.text)
            }
            if (element.lambdaArguments.isNotEmpty()) {
                appendFixedText(lambda.text)
            }
        }.let { (parentExclPrefixExpression ?: callOrQualified).replaced(it) }
    }

    private fun KtCallExpression.lambda(): KtLambdaExpression? {
        return lambdaArguments.singleOrNull()?.getArgumentExpression().safeAs() ?: getLastLambdaExpression()
    }

    private fun PsiElement.asExclPrefixExpression(): KtPrefixExpression? {
        return safeAs<KtPrefixExpression>()?.takeIf { it.operationToken == KtTokens.EXCL && it.baseExpression != null }
    }

    private fun KtExpression?.removeUnnecessaryParentheses() {
        if (this !is KtParenthesizedExpression) return
        val innerExpression = this.expression ?: return
        if (KtPsiUtil.areParenthesesUseless(this)) {
            this.replace(innerExpression)
        }
    }

    companion object {
        private val collectionFunctions = listOf("all", "any", "none", "filter", "filterNot", "filterTo", "filterNotTo").associateWith {
            listOf(FqName("kotlin.collections.$it"), FqName("kotlin.sequences.$it"))
        }

        private val standardFunctions = listOf("takeIf", "takeUnless").associateWith {
            listOf(FqName("kotlin.$it"))
        }

        private val functions = collectionFunctions + standardFunctions
    }
}

class ConvertAllToAnyIntention : ConvertFilteringFunctionWithDemorgansLawIntention("all", "any", true, true)
class ConvertAnyToAllIntention : ConvertFilteringFunctionWithDemorgansLawIntention("any", "all", true, true)

class ConvertAnyToNoneIntention : ConvertFilteringFunctionWithDemorgansLawIntention("any", "none", true, false)
class ConvertNoneToAnyIntention : ConvertFilteringFunctionWithDemorgansLawIntention("none", "any", true, false)

class ConvertAllToNoneIntention : ConvertFilteringFunctionWithDemorgansLawIntention("all", "none", false, true)
class ConvertNoneToAllIntention : ConvertFilteringFunctionWithDemorgansLawIntention("none", "all", false, true)

class ConvertFilterToFilterNotIntention : ConvertFilteringFunctionWithDemorgansLawIntention("filter", "filterNot", false, true)
class ConvertFilterNotToFilterIntention : ConvertFilteringFunctionWithDemorgansLawIntention("filterNot", "filter", false, true)

class ConvertFilterToToFilterNotToIntention : ConvertFilteringFunctionWithDemorgansLawIntention("filterTo", "filterNotTo", false, true)
class ConvertFilterNotToToFilterToIntention : ConvertFilteringFunctionWithDemorgansLawIntention("filterNotTo", "filterTo", false, true)

class ConvertTakeIfToTakeUnlessIntention : ConvertFilteringFunctionWithDemorgansLawIntention("takeIf", "takeUnless", false, true)
class ConvertTakeUnlessToTakeIfIntention : ConvertFilteringFunctionWithDemorgansLawIntention("takeUnless", "takeIf", false, true)
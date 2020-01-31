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

package org.jetbrains.kotlin.idea.inspections.conventionNameCalls

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.inspections.KotlinEqualsBetweenInconvertibleTypesInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.isAnyEquals
import org.jetbrains.kotlin.idea.intentions.isOperatorOrCompatible
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.calleeTextRangeInThis
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.getKotlinTypeWithPossibleSmartCastToFP
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReplaceCallWithBinaryOperatorInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {

    private fun IElementType.inverted(): KtSingleValueToken? = when (this) {
        KtTokens.LT -> KtTokens.GT
        KtTokens.GT -> KtTokens.LT

        KtTokens.GTEQ -> KtTokens.LTEQ
        KtTokens.LTEQ -> KtTokens.GTEQ

        else -> null
    }

    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        val calleeExpression = element.callExpression?.calleeExpression as? KtSimpleNameExpression ?: return false
        if (operation(calleeExpression) == null) return false

        val context = element.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = element.callExpression?.getResolvedCall(context) ?: return false
        if (!resolvedCall.isReallySuccess()) return false
        if (resolvedCall.call.typeArgumentList != null) return false
        val argument = resolvedCall.call.valueArguments.singleOrNull() ?: return false
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.index != 0) return false

        if (!element.isReceiverExpressionWithValue()) return false

        val (expressionToBeReplaced, newExpression) = getReplacementExpression(element) ?: return false
        val newContext = newExpression.analyzeAsReplacement(expressionToBeReplaced, context)
        return newContext.diagnostics.noSuppression().forElement(newExpression).isEmpty()
    }

    override fun inspectionHighlightRangeInElement(element: KtDotQualifiedExpression) = element.calleeTextRangeInThis()

    override fun inspectionHighlightType(element: KtDotQualifiedExpression): ProblemHighlightType {
        val calleeExpression = element.callExpression?.calleeExpression as? KtSimpleNameExpression
        val identifier = calleeExpression?.getReferencedNameAsName()
        if (element.platform.isJs() && identifier == OperatorNameConventions.EQUALS) {
            val context = element.analyze(BodyResolveMode.PARTIAL)
            if (element.receiverExpression.getType(context)?.isDynamic() == true) {
                return ProblemHighlightType.INFORMATION
            }
        }
        val isFloatingPointNumberEquals = calleeExpression?.isFloatingPointNumberEquals() ?: false
        return if (isFloatingPointNumberEquals) {
            ProblemHighlightType.INFORMATION
        } else if (identifier == OperatorNameConventions.EQUALS || identifier == OperatorNameConventions.COMPARE_TO) {
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        } else {
            ProblemHighlightType.INFORMATION
        }
    }

    override fun inspectionText(element: KtDotQualifiedExpression) = KotlinBundle.message("call.replaceable.with.binary.operator")

    override val defaultFixText: String get() = KotlinBundle.message("replace.with.binary.operator")

    override fun fixText(element: KtDotQualifiedExpression): String {
        val calleeExpression = element.callExpression?.calleeExpression as? KtSimpleNameExpression ?: return defaultFixText
        if (calleeExpression.isFloatingPointNumberEquals()) {
            return KotlinBundle.message("replace.total.order.equality.with.ieee.754.equality")
        }

        val operation = operation(calleeExpression) ?: return defaultFixText
        return KotlinBundle.message("replace.with.0", operation.value)
    }

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val (expressionToBeReplaced, newExpression) = getReplacementExpression(element) ?: return
        expressionToBeReplaced.replace(newExpression)
    }

    private fun getReplacementExpression(element: KtDotQualifiedExpression): Pair<KtExpression, KtExpression>? {
        val callExpression = element.callExpression ?: return null
        val calleeExpression = callExpression.calleeExpression as? KtSimpleNameExpression ?: return null
        val operation = operation(calleeExpression) ?: return null
        val argument = callExpression.valueArguments.single().getArgumentExpression() ?: return null
        val receiver = element.receiverExpression

        val factory = KtPsiFactory(element)
        return when (operation) {
            KtTokens.EXCLEQ -> {
                val prefixExpression = element.getWrappingPrefixExpressionIfAny() ?: return null
                val newExpression = factory.createExpressionByPattern("$0 != $1", receiver, argument)
                prefixExpression to newExpression
            }
            in OperatorConventions.COMPARISON_OPERATIONS -> {
                val binaryParent = element.parent as? KtBinaryExpression ?: return null
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                binaryParent to newExpression
            }
            else -> {
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                element to newExpression
            }
        }
    }

    private fun PsiElement.getWrappingPrefixExpressionIfAny() =
        (getLastParentOfTypeInRow<KtParenthesizedExpression>() ?: this).parent as? KtPrefixExpression

    private fun operation(calleeExpression: KtSimpleNameExpression): KtSingleValueToken? {
        val identifier = calleeExpression.getReferencedNameAsName()
        val dotQualified = calleeExpression.parent.parent as? KtDotQualifiedExpression ?: return null
        val isOperatorOrCompatible by lazy {
            (calleeExpression.resolveToCall()?.resultingDescriptor as? FunctionDescriptor)?.isOperatorOrCompatible == true
        }
        return when (identifier) {
            OperatorNameConventions.EQUALS -> {
                if (!dotQualified.isAnyEquals()) return null
                with(KotlinEqualsBetweenInconvertibleTypesInspection) {
                    val receiver = dotQualified.receiverExpression
                    val argument = dotQualified.callExpression?.valueArguments?.singleOrNull()?.getArgumentExpression()
                    if (dotQualified.analyze(BodyResolveMode.PARTIAL).isInconvertibleTypes(receiver, argument)) return null
                }
                val prefixExpression = dotQualified.getWrappingPrefixExpressionIfAny()
                if (prefixExpression != null && prefixExpression.operationToken == KtTokens.EXCL) KtTokens.EXCLEQ
                else KtTokens.EQEQ
            }
            OperatorNameConventions.COMPARE_TO -> {
                if (!isOperatorOrCompatible) return null
                // callee -> call -> DotQualified -> Binary
                val binaryParent = dotQualified.parent as? KtBinaryExpression ?: return null
                val notZero = when {
                    binaryParent.right?.text == "0" -> binaryParent.left
                    binaryParent.left?.text == "0" -> binaryParent.right
                    else -> return null
                }
                if (notZero != dotQualified) return null
                val token = binaryParent.operationToken as? KtSingleValueToken ?: return null
                if (token in OperatorConventions.COMPARISON_OPERATIONS) {
                    if (notZero == binaryParent.left) token else token.inverted()
                } else {
                    null
                }
            }
            else -> {
                if (!isOperatorOrCompatible) return null
                OperatorConventions.BINARY_OPERATION_NAMES.inverse()[identifier]
            }
        }
    }

    private fun KtDotQualifiedExpression.isFloatingPointNumberEquals(): Boolean {
        val resolvedCall = resolveToCall() ?: return false
        val context = analyze(BodyResolveMode.PARTIAL)
        val declarationDescriptor = containingDeclarationForPseudocode?.resolveToDescriptorIfAny()
        val dataFlowValueFactory = getResolutionFacade().getFrontendService(DataFlowValueFactory::class.java)
        val defaultType: (KotlinType, Set<KotlinType>) -> KotlinType = { givenType, stableTypes -> stableTypes.firstOrNull() ?: givenType }
        val receiverType = resolvedCall.getReceiverExpression()?.getKotlinTypeWithPossibleSmartCastToFP(
            context, declarationDescriptor, languageVersionSettings, dataFlowValueFactory, defaultType
        ) ?: return false
        val argumentType = resolvedCall.getFirstArgumentExpression()?.getKotlinTypeWithPossibleSmartCastToFP(
            context, declarationDescriptor, languageVersionSettings, dataFlowValueFactory, defaultType
        ) ?: return false
        return receiverType.isFpType() && argumentType.isNumericType() ||
                argumentType.isFpType() && receiverType.isNumericType()
    }

    private fun KtSimpleNameExpression.isFloatingPointNumberEquals(): Boolean {
        val dotQualified = parent.parent as? KtDotQualifiedExpression ?: return false
        return dotQualified.isFloatingPointNumberEquals()
    }

    private fun KotlinType.isFpType(): Boolean {
        return isFloat() || isDouble()
    }

    private fun KotlinType.isNumericType(): Boolean {
        return isFpType() || isByte() || isShort() || isInt() || isLong()
    }
}
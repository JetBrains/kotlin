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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.isAnyEquals
import org.jetbrains.kotlin.idea.intentions.isOperatorOrCompatible
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.getKotlinTypeWithPossibleSmartCastToFP
import org.jetbrains.kotlin.resolve.isJs
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

        val resolvedCall = element.toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
        if (!resolvedCall.isReallySuccess()) return false
        if (resolvedCall.call.typeArgumentList != null) return false
        val argument = resolvedCall.call.valueArguments.singleOrNull() ?: return false
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.index != 0) return false

        return element.isReceiverExpressionWithValue()
    }

    override fun inspectionTarget(element: KtDotQualifiedExpression) = element.callExpression?.calleeExpression ?: element

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

    override fun inspectionText(element: KtDotQualifiedExpression) = "Call replaceable with binary operator"

    override val defaultFixText: String
        get() = "Replace with binary operator"

    override fun fixText(element: KtDotQualifiedExpression): String {
        val calleeExpression = element.callExpression?.calleeExpression as? KtSimpleNameExpression ?: return defaultFixText
        if (calleeExpression.isFloatingPointNumberEquals()) {
            return "Replace total order equality with IEEE 754 equality"
        }

        val operation = operation(calleeExpression) ?: return defaultFixText
        return "Replace with '${operation.value}'"
    }

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val qualifiedExpression = element.getParentOfType<KtDotQualifiedExpression>(strict = false) ?: return
        val callExpression = qualifiedExpression.callExpression ?: return
        val operation = operation(callExpression.calleeExpression as? KtSimpleNameExpression ?: return) ?: return
        val argument = callExpression.valueArguments.single().getArgumentExpression() ?: return
        val receiver = qualifiedExpression.receiverExpression

        val factory = KtPsiFactory(qualifiedExpression)
        when (operation) {
            KtTokens.EXCLEQ -> {
                val prefixExpression = qualifiedExpression.getWrappingPrefixExpressionIfAny() ?: return
                val newExpression = factory.createExpressionByPattern("$0 != $1", receiver, argument)
                prefixExpression.replace(newExpression)
            }
            in OperatorConventions.COMPARISON_OPERATIONS -> {
                val binaryParent = qualifiedExpression.parent as? KtBinaryExpression ?: return
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                binaryParent.replace(newExpression)
            }
            else -> {
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                qualifiedExpression.replace(newExpression)
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
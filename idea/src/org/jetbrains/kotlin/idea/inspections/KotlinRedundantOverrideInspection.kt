/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class KotlinRedundantOverrideInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            namedFunctionVisitor(fun(function) {
                val funKeyword = function.funKeyword ?: return
                val modifierList = function.modifierList ?: return
                if (!modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
                if (MODIFIER_EXCLUDE_OVERRIDE.any { modifierList.hasModifier(it) }) return
                if (function.annotationEntries.isNotEmpty()) return
                if (function.containingClass()?.isData() == true) return

                val bodyExpression = function.bodyExpression ?: return
                val qualifiedExpression = when (bodyExpression) {
                    is KtDotQualifiedExpression -> bodyExpression
                    is KtBlockExpression -> {
                        val body = bodyExpression.statements.singleOrNull()
                        when (body) {
                            is KtReturnExpression -> body.returnedExpression
                            is KtDotQualifiedExpression -> body.takeIf {
                                function.typeReference.let { it == null || it.text == "Unit" }
                            }
                            else -> null
                        }

                    }
                    else -> null
                } as? KtDotQualifiedExpression ?: return

                val superExpression = qualifiedExpression.receiverExpression as? KtSuperExpression ?: return
                if (superExpression.superTypeQualifier != null) return

                val superCallElement = qualifiedExpression.selectorExpression as? KtCallElement ?: return
                if (!isSameFunctionName(superCallElement, function)) return
                if (!isSameArguments(superCallElement, function)) return
                if (function.isDefinedInDelegatedSuperType(qualifiedExpression)) return

                val descriptor = holder.manager.createProblemDescriptor(
                        function,
                        TextRange(modifierList.startOffsetInParent, funKeyword.endOffset - function.startOffset),
                        "Redundant overriding method",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        isOnTheFly,
                        RedundantOverrideFix()
                )
                holder.registerProblem(descriptor)
            })

    private fun isSameArguments(superCallElement: KtCallElement, function: KtNamedFunction): Boolean {
        val arguments = superCallElement.valueArguments
        val parameters = function.valueParameters
        if (arguments.size != parameters.size) return false
        return arguments.zip(parameters).all { (argument, parameter) ->
            argument.getArgumentExpression()?.text == parameter.name
        }
    }

    private fun isSameFunctionName(superSelectorExpression: KtCallElement, function: KtNamedFunction): Boolean {
        val superCallMethodName = superSelectorExpression.getCallNameExpression()?.text ?: return false
        return function.name == superCallMethodName
    }

    private class RedundantOverrideFix : LocalQuickFix {
        override fun getName() = "Remove redundant overriding method"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }

    companion object {
        private val MODIFIER_EXCLUDE_OVERRIDE = KtTokens.MODIFIER_KEYWORDS_ARRAY.asList() - KtTokens.OVERRIDE_KEYWORD
    }
}

private fun KtNamedFunction.isDefinedInDelegatedSuperType(superQualifiedExpression: KtDotQualifiedExpression): Boolean {
    val delegatedSuperTypeEntries =
            containingClassOrObject?.superTypeListEntries?.filterIsInstance<KtDelegatedSuperTypeEntry>() ?: return false
    if (delegatedSuperTypeEntries.isEmpty()) return false

    val context = superQualifiedExpression.analyze()
    val delegatedSuperTypes = delegatedSuperTypeEntries.mapNotNull { entry ->
        context[BindingContext.TYPE, entry.typeReference]
    }

    val superResolvedCall = superQualifiedExpression.getResolvedCall(context) ?: return false
    val superCallResolvedDescriptor = superResolvedCall.resultingDescriptor
    val superCallResolvedReceiverTypes = superCallResolvedDescriptor.overriddenDescriptors.mapNotNull { it.dispatchReceiverParameter?.type }

    return delegatedSuperTypes.any { delegatedSuperType ->
        superCallResolvedReceiverTypes.any { superCallReceiverType ->
            delegatedSuperType.isSubtypeOf(superCallReceiverType)
        }
    }
}

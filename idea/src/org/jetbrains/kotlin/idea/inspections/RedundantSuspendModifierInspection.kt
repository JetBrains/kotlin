/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.highlighter.hasSuspendCalls
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext

class RedundantSuspendModifierInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return namedFunctionVisitor(fun(function) {
            if (!function.languageVersionSettings.supportsFeature(LanguageFeature.Coroutines)) return

            val suspendModifier = function.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) ?: return
            if (!function.hasBody()) return
            if (function.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return

            val context = function.analyzeWithContent()
            val descriptor = context[BindingContext.FUNCTION, function] ?: return
            if (descriptor.modality == Modality.OPEN) return

            if (function.hasSuspendCalls(context)) return

            holder.registerProblem(
                suspendModifier,
                KotlinBundle.message("redundant.suspend.modifier"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntentionWrapper(
                    RemoveModifierFix(function, KtTokens.SUSPEND_KEYWORD, isRedundant = true),
                    function.containingFile
                )
            )
        })
    }

    private fun KtNamedFunction.hasSuspendCalls(context: BindingContext): Boolean {
        return anyDescendantOfType<KtExpression> {
            if (it is KtNameReferenceExpression && it.getReferencedName() == this.name && it.mainReference.resolve() == this) {
                return@anyDescendantOfType false
            }
            it.hasSuspendCalls(context)
        }
    }
}

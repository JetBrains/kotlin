/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.declarationVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class RedundantVisibilityModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return declarationVisitor(fun(declaration: KtDeclaration) {
            val isInApiMode = declaration.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) != ExplicitApiMode.DISABLED

            if (declaration is KtPropertyAccessor && declaration.isGetter) return // There is a quick fix for REDUNDANT_MODIFIER_IN_GETTER
            val visibilityModifier = declaration.visibilityModifier() ?: return
            val implicitVisibility = declaration.implicitVisibility()
            if (isInApiMode && (declaration.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility)?.isEffectivelyPublicApi == true) return@declarationVisitor

            val redundantVisibility = when {
                visibilityModifier.node.elementType == implicitVisibility ->
                    implicitVisibility
                declaration.hasModifier(KtTokens.INTERNAL_KEYWORD) && declaration.containingClassOrObject?.isLocal == true ->
                    KtTokens.INTERNAL_KEYWORD
                else ->
                    null
            } ?: return

            if (redundantVisibility == KtTokens.PUBLIC_KEYWORD
                && declaration is KtProperty
                && declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                && declaration.isVar
                && declaration.setterVisibility().let { it != null && it != Visibilities.PUBLIC }
            ) return

            holder.registerProblem(
                visibilityModifier,
                "Redundant visibility modifier",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntentionWrapper(
                    RemoveModifierFix(declaration, redundantVisibility, isRedundant = true),
                    declaration.containingFile
                )
            )
        })
    }

    private fun KtProperty.setterVisibility(): Visibility? {
        val descriptor = descriptor as? PropertyDescriptor ?: return null
        if (setter?.visibilityModifier() != null) {
            val visibility = descriptor.setter?.visibility
            if (visibility != null) return visibility
        }
        return (descriptor as? CallableMemberDescriptor)
            ?.overriddenDescriptors
            ?.firstNotNullResult { (it as? PropertyDescriptor)?.setter }
            ?.visibility
    }
}

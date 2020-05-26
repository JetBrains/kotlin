/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.deprecation.deprecatedByOverriddenMessage

class OverridingDeprecatedMemberInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
                registerProblemIfNeeded(declaration, declaration.nameIdentifier ?: return)
            }

            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                if (!accessor.property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
                registerProblemIfNeeded(accessor, accessor.namePlaceholder)
            }

            private fun registerProblemIfNeeded(declaration: KtDeclaration, targetForProblem: PsiElement) {
                val resolutionFacade = declaration.getResolutionFacade()
                val accessorDescriptor = declaration.resolveToDescriptorIfAny(resolutionFacade) as? CallableMemberDescriptor ?: return

                val deprecationProvider = resolutionFacade.frontendService<DeprecationResolver>()

                val message = deprecationProvider.getDeprecations(accessorDescriptor)
                    .firstOrNull()
                    ?.deprecatedByOverriddenMessage() ?: return
                val problem = holder.manager.createProblemDescriptor(
                    targetForProblem,
                    message,
                    /* showTooltip = */ true,
                    ProblemHighlightType.LIKE_DEPRECATED,
                    isOnTheFly
                )
                holder.registerProblem(problem)
            }
        }
    }
}

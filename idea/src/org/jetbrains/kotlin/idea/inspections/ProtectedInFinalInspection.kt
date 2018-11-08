/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ProtectedInFinalInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return declarationVisitor(fun(declaration) {
            val visibilityModifier = declaration.visibilityModifier() ?: return
            val modifierType = visibilityModifier.node?.elementType
            if (modifierType == KtTokens.PROTECTED_KEYWORD) {
                val parentClass = declaration.getParentOfType<KtClass>(true) ?: return
                if (!parentClass.isInheritable() && !parentClass.isEnum() &&
                    declaration.implicitVisibility() != KtTokens.PROTECTED_KEYWORD &&
                    !declaration.isFinalizeMethod()
                ) {
                    holder.registerProblem(
                        visibilityModifier,
                        "'protected' visibility is effectively 'private' in a final class",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        MakePrivateFix(),
                        MakeOpenFix()
                    )
                }
            }
        })
    }

    private fun KtDeclaration.isFinalizeMethod(): Boolean {
        val function = this as? KtNamedFunction ?: return false
        return function.name == "finalize"
                && function.valueParameters.isEmpty()
                && (function.descriptor as? FunctionDescriptor)?.returnType?.isUnit() == true
    }

    class MakePrivateFix : LocalQuickFix {
        override fun getName(): String = "Make private"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val modifierListOwner = descriptor.psiElement.getParentOfType<KtModifierListOwner>(true)
                ?: throw IllegalStateException("Can't find modifier list owner for modifier")
            addModifier(modifierListOwner, KtTokens.PRIVATE_KEYWORD)
        }
    }

    class MakeOpenFix : LocalQuickFix {
        override fun getName(): String = "Make class open"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val modifierListOwner = descriptor.psiElement.getParentOfType<KtModifierListOwner>(true)
                ?: throw IllegalStateException("Can't find modifier list owner for modifier")
            val parentClass = modifierListOwner.getParentOfType<KtClass>(true) ?: return
            addModifier(parentClass, KtTokens.OPEN_KEYWORD)
        }
    }
}

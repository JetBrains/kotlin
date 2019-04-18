/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateEqualsAndHashcodeAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.classVisitor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class ArrayInDataClassInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return classVisitor { klass ->
            if (!klass.isData()) return@classVisitor
            val constructor = klass.primaryConstructor ?: return@classVisitor
            if (hasOverriddenEqualsAndHashCode(klass)) return@classVisitor
            val context = constructor.analyze(BodyResolveMode.PARTIAL)
            for (parameter in constructor.valueParameters) {
                if (!parameter.hasValOrVar()) continue
                val type = context.get(BindingContext.TYPE, parameter.typeReference) ?: continue
                if (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) {
                    holder.registerProblem(parameter,
                                           "Array property in data class: it's recommended to override equals() / hashCode()",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           GenerateEqualsAndHashcodeFix())
                }
            }
        }
    }

    private fun hasOverriddenEqualsAndHashCode(klass: KtClass): Boolean {
        var overriddenEquals = false
        var overriddenHashCode = false
        for (declaration in klass.declarations) {
            if (declaration !is KtFunction) continue
            if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) continue
            if (declaration.nameAsName == OperatorNameConventions.EQUALS && declaration.valueParameters.size == 1) {
                val type = (declaration.resolveToDescriptorIfAny() as? FunctionDescriptor)?.valueParameters?.singleOrNull()?.type
                if (type != null && KotlinBuiltIns.isNullableAny(type)) {
                    overriddenEquals = true
                }
            }
            if (declaration.name == "hashCode" && declaration.valueParameters.size == 0) {
                overriddenHashCode = true
            }
        }
        return overriddenEquals && overriddenHashCode
    }

    class GenerateEqualsAndHashcodeFix : LocalQuickFix {
        override fun getName() = "Generate equals() and hashCode()"

        override fun getFamilyName() = name

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            descriptor.psiElement.getNonStrictParentOfType<KtClass>()?.run {
                KotlinGenerateEqualsAndHashcodeAction().doInvoke(project, descriptor.psiElement.findExistingEditor(), this)
            }
        }
    }
}

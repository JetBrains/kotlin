/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateEqualsAndHashcodeAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class ArrayInDataClassInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (!klass.isData()) return
                val constructor = klass.primaryConstructor ?: return
                if (hasOverriddenEqualsAndHashCode(klass)) return
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

            private fun hasOverriddenEqualsAndHashCode(klass: KtClass): Boolean {
                var overriddenEquals = false
                var overriddenHashCode = false
                for (declaration in klass.declarations) {
                    if (declaration !is KtFunction) continue
                    if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) continue
                    if (declaration.nameAsName == OperatorNameConventions.EQUALS && declaration.valueParameters.size == 1) {
                        val parameter = declaration.valueParameters.single()
                        val context = declaration.analyze(BodyResolveMode.PARTIAL)
                        val type = context.get(BindingContext.TYPE, parameter.typeReference)
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
        }
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

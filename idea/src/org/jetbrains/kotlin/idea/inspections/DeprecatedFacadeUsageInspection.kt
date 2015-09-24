/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

public class DeprecatedFacadeUsageInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                val resolveResult = expression.reference?.resolve()
                if (resolveResult is PsiMethod && resolveResult.findDelegatedMethodAnnotation() != null) {
                    holder.registerProblem(expression, "Use of deprecated package facade class",
                                           ProblemHighlightType.LIKE_DEPRECATED,
                                           DeprecatedFacadeUsageFix())
                }
            }

        }
    }
}
private fun PsiMethod.findDelegatedMethodAnnotation() =
        modifierList.findAnnotation(JvmAnnotationNames.KOTLIN_DELEGATED_METHOD.asString())

public class DeprecatedFacadeUsageFix : LocalQuickFix {
    override fun getName(): String = "Replace with new-style facade class"
    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val reference = descriptor.psiElement.reference ?: return
        val resolveResult = reference.resolve() as? PsiMethod ?: return

        val delegatedMethod = resolveResult.findDelegatedMethodAnnotation() ?: return
        val attr = delegatedMethod.findAttributeValue(JvmAnnotationNames.IMPLEMENTATION_CLASS_NAME_FIELD_NAME)
        val targetClassName = (attr as? PsiLiteral)?.value as? String ?: return

        val targetClass = JavaPsiFacade.getInstance(project).findClass(targetClassName, project.allScope()) ?: return
        val targetMethod = targetClass.findMethodBySignature(resolveResult, false) ?: return

        val newReference = reference.bindToElement(targetMethod)
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newReference)

        val javaFile = newReference.containingFile as? PsiJavaFile
        if (javaFile != null) {
            JavaCodeStyleManager.getInstance(project).removeRedundantImports(javaFile)
        }
    }
}

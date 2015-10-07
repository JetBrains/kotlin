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
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.JetObjectDeclaration

public class DeprecatedObjectInstanceFieldReferenceInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                val resolvedTo = expression.reference?.resolve()
                if (!(resolvedTo is PsiField && resolvedTo.name == JvmAbi.DEPRECATED_INSTANCE_FIELD)) return

                val containingClass = resolvedTo.containingClass
                if (containingClass !is KotlinLightClass || containingClass.getOrigin() !is JetObjectDeclaration) return

                holder.registerProblem(
                        expression, "Use of deprecated '${JvmAbi.DEPRECATED_INSTANCE_FIELD}' field",
                        ProblemHighlightType.LIKE_DEPRECATED,
                        DeprecatedObjectInstanceFieldReferenceFix()
                )
            }
        }
    }
}

public class DeprecatedObjectInstanceFieldReferenceFix : LocalQuickFix {
    override fun getName(): String = "Replace with reference to '${JvmAbi.INSTANCE_FIELD}' field"
    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val reference = descriptor.psiElement.reference ?: return
        val deprecatedField = reference.resolve() as? PsiField ?: return

        val lightClassForObject = deprecatedField.containingClass as? KotlinLightClass ?: return
        val correctField = lightClassForObject.findFieldByName(JvmAbi.INSTANCE_FIELD, false) ?: return

        val newReference = reference.bindToElement(correctField)

        val codeStyleManager = JavaCodeStyleManager.getInstance(project)
        codeStyleManager.shortenClassReferences(newReference)
        val javaFile = newReference.containingFile as? PsiJavaFile ?: return
        codeStyleManager.removeRedundantImports(javaFile)
    }
}

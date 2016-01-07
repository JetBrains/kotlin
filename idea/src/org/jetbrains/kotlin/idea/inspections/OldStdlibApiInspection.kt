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
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class OldStdlibApiInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.language == KotlinLanguage.INSTANCE) return
                val importStatement = element.getParentOfType<PsiImportStatement>(false)
                if (importStatement != null) return

                for (reference in element.references) {
                    checkReference(reference)?.let {
                        holder.registerProblem(element, "Usage of the Kotlin standard library through a deprecated qualified name",
                                               ProblemHighlightType.LIKE_DEPRECATED, it)

                    }
                }
            }
        }
    }

    private fun checkReference(reference: PsiReference): LocalQuickFix? {
        val resolveResult = reference.resolve()
        if (resolveResult is PsiClass) {
            val fqName = resolveResult.qualifiedName
            val newFqName = StdlibMigrationMap.classMap[fqName]
            if (newFqName != null) {
                return OldStdlibApiFix { project, scope ->
                    JavaPsiFacade.getInstance(project).findClass(newFqName, scope)
                }
            }
        }
        else if (resolveResult is PsiMethod) {
            val containingClass = resolveResult.containingClass ?: return null
            val fqName = MethodFQName(containingClass.qualifiedName ?: return null, resolveResult.name)
            val newFqName = StdlibMigrationMap.methodMap[fqName]
            if (newFqName != null) {
                return OldStdlibApiFix { project, scope ->
                    JavaPsiFacade.getInstance(project).findClass(newFqName.className, scope)
                            ?.findMethodsByName(newFqName.methodName, false)
                            ?.singleOrNull()
                }
            }
        }
        return null
    }
}

class OldStdlibApiFix(val newElementCallback: (Project, GlobalSearchScope) -> PsiElement?) : LocalQuickFix {
    override fun getName(): String = "Replace with new qualified name"
    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val reference = element.reference ?: return
        val module = ModuleUtil.findModuleForPsiElement(element)
        val scope = module?.moduleWithLibrariesScope ?: project.allScope()

        val newElement = newElementCallback(project, scope) ?: return
        val newReference = reference.bindToElement(newElement)

        val javaFile = newReference.containingFile as? PsiJavaFile
        if (javaFile != null) {
            JavaCodeStyleManager.getInstance(project).removeRedundantImports(javaFile)
        }

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newReference)
    }
}

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
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.java.lazy.DeprecatedFunctionClassFqNameParser
import java.util.ArrayList

public class ReplaceDeprecatedFunctionClassUsages : LocalInspectionTool() {
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (isOnTheFly || !ProjectRootsUtil.isInProjectSource(file) || file !is PsiJavaFile) {
            return null
        }
        return checkFile(file, manager)?.let { arrayOf(it) }
    }

    public fun checkFile(file: PsiJavaFile, manager: InspectionManager): ProblemDescriptor? {
        val references = ArrayList<PsiJavaCodeReferenceElement>(0)

        file.acceptChildren(object : JavaRecursiveElementVisitor() {
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                if ("Function" in reference.getText() && extractFunctionClassFqName(reference) != null) {
                    references.add(reference)
                }

                super.visitElement(reference)
            }

            override fun visitImportList(list: PsiImportList?) {
                // Skip import list for simplicity, update all other references instead and invoke "Optimize Imports"
            }
        })

        return if (references.isEmpty()) null else manager.createProblemDescriptor(
                file, MESSAGE, false, arrayOf(Fix(references)), ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
    }

    private class Fix(val references: List<PsiJavaCodeReferenceElement>) : LocalQuickFix {
        override fun getName() = MESSAGE
        override fun getFamilyName() = MESSAGE

        override fun applyFix(project: Project, problem: ProblemDescriptor) {
            val psiFacade = JavaPsiFacade.getInstance(project)
            val codeStyleManager = JavaCodeStyleManager.getInstance(project)

            val newReferences = ArrayList<PsiElement>(references.size())
            for (reference in references) {
                val newFqName = extractFunctionClassFqName(reference) ?: continue
                val newClass = psiFacade.findClass(newFqName, reference.getResolveScope())
                if (newClass != null) {
                    newReferences.add(reference.bindToElement(newClass))
                }
            }

            codeStyleManager.optimizeImports(problem.getPsiElement().getContainingFile())

            for (reference in newReferences) {
                codeStyleManager.shortenClassReferences(reference)
            }
        }
    }

    companion object {
        val MESSAGE = "Replace usages of deprecated Kotlin function classes in Java sources"

        fun extractFunctionClassFqName(reference: PsiReference): String? {
            val fqName = (reference.resolve() as? ClsClassImpl)?.getQualifiedName() ?: return null
            return DeprecatedFunctionClassFqNameParser.extractOldAndNewFqName(fqName)?.second
        }
    }
}

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

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.KotlinImportOptimizer
import org.jetbrains.kotlin.idea.imports.importableFqNameSafe
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import java.util.HashSet

class UnusedImportInspection : AbstractKotlinInspection() {
    private val visitorKey = Key<KotlinImportOptimizer.CollectUsedDescriptorsVisitor>("UnusedImportInspection.visitorKey")

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        val visitor = KotlinImportOptimizer.CollectUsedDescriptorsVisitor(session.getFile() as JetFile, recursive = false)
        session.putUserData(visitorKey, visitor)
        return visitor
    }

    override fun inspectionFinished(session: LocalInspectionToolSession, problemsHolder: ProblemsHolder) {
        val usedDescriptors = session.getUserData(visitorKey)!!.descriptors

        val fqNames = HashSet<FqName>()
        val parentFqNames = HashSet<FqName>()
        for (descriptor in usedDescriptors) {
            val fqName = descriptor.importableFqNameSafe
            fqNames.add(fqName)
            val parentFqName = fqName.parent()
            if (!parentFqName.isRoot()) {
                parentFqNames.add(parentFqName)
            }
        }

        val file = session.getFile() as JetFile
        val directives = file.getImportDirectives()
        for (directive in directives) {
            val importPath = directive.getImportPath() ?: continue
            if (importPath.getAlias() != null) continue // highlighting of unused alias imports not supported yet
            val isUsed = if (importPath.isAllUnder()) {
                importPath.fqnPart() in parentFqNames
            }
            else {
                importPath.fqnPart() in fqNames
            }

            if (!isUsed) {
                val nameExpression = directive.getImportedReference()?.getQualifiedElementSelector() as? JetSimpleNameExpression
                if (nameExpression == null || nameExpression.getReferenceTargets(nameExpression.analyze()).isEmpty()) continue // do not highlight unresolved imports as unused

                problemsHolder.registerProblem(
                        directive,
                        "Unused import directive",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        OptimizeImportsQuickFix(file))
            }
        }
    }

    private class OptimizeImportsQuickFix(private val file: JetFile): LocalQuickFix {
        override fun getName() = "Optimize imports"

        override fun getFamilyName() = getName()

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            OptimizeImportsProcessor(project, file).run()
        }
    }

}
/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.RefactoringHelper
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SequentialModalProgressTask
import com.intellij.util.SequentialTask
import org.jetbrains.kotlin.idea.inspections.KotlinUnusedImportInspection
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

// Based on com.intellij.refactoring.OptimizeImportsRefactoringHelper
class KotlinOptimizeImportsRefactoringHelper : RefactoringHelper<Set<KtFile>> {
    internal class CollectUnusedImportsTask(
        private val task: SequentialModalProgressTask,
        private val dumbService: DumbService,
        private val unusedImports: MutableSet<SmartPsiElementPointer<KtImportDirective>>,
        operationData: Set<KtFile>
    ) : SequentialTask {
        private val myTotalCount = operationData.size
        private val operationIterator = operationData.withIndex().iterator()

        override fun prepare() {}

        override fun stop() {}

        override fun isDone(): Boolean = !operationIterator.hasNext()

        override fun iteration(): Boolean {
            val (counter, file) = operationIterator.next()
            if (!file.isValid) return isDone
            val virtualFile = file.virtualFile ?: return isDone

            with(task.indicator) {
                text2 = virtualFile.presentableUrl
                fraction = counter.toDouble() / myTotalCount
            }

            dumbService.runReadActionInSmartMode {
                KotlinUnusedImportInspection.analyzeImports(file)?.unusedImports?.mapTo(unusedImports) { it.createSmartPointer() }
            }
            return isDone
        }
    }

    internal class OptimizeImportsTask(
        private val task: SequentialModalProgressTask,
        pointers: Set<SmartPsiElementPointer<KtImportDirective>>
    ) : SequentialTask {
        private val pointerIterator = pointers.withIndex().iterator()
        private val myTotal: Int = pointers.size

        override fun prepare() {}

        override fun isDone() = !pointerIterator.hasNext()

        override fun iteration(): Boolean {
            val (counter, pointer) = pointerIterator.next()

            task.indicator?.fraction = counter.toDouble() / myTotal

            val directive = pointer.element
            if (directive?.isValid == true) {
                task.indicator?.text2 = directive.containingFile.virtualFile.presentableUrl
                runWriteAction {
                    try {
                        directive.delete()
                    } catch (e: IncorrectOperationException) {
                        LOG.error(e)
                    }
                }
            }

            return isDone
        }

        override fun stop() {}

        companion object {
            private val LOG = Logger.getInstance("#" + OptimizeImportsTask::class.java.name)
        }
    }

    companion object {
        private const val COLLECT_UNUSED_IMPORTS_TITLE = "Collect unused imports"
        private const val REMOVING_REDUNDANT_IMPORTS_TITLE = "Removing redundant imports"
    }

    override fun prepareOperation(usages: Array<UsageInfo>): Set<KtFile> {
        return usages.mapNotNullTo(LinkedHashSet<KtFile>()) {
            if (!it.isNonCodeUsage) it.file as? KtFile else null
        }
    }

    override fun performOperation(project: Project, operationData: Set<KtFile>) {
        CodeStyleManager.getInstance(project).performActionWithFormatterDisabled {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        if (operationData.isEmpty()) return

        val unusedImports = mutableSetOf<SmartPsiElementPointer<KtImportDirective>>()

        val progressManager = ProgressManager.getInstance()

        val dumbService = DumbService.getInstance(project)

        val collectTask = object : SequentialModalProgressTask(project, COLLECT_UNUSED_IMPORTS_TITLE, false) {
            override fun onSuccess() {
                val progressTask = SequentialModalProgressTask(project, REMOVING_REDUNDANT_IMPORTS_TITLE, false)
                with(progressTask) {
                    setMinIterationTime(200)
                    setTask(OptimizeImportsTask(this, unusedImports))
                }
                progressManager.run(progressTask)
            }
        }
        with(collectTask) {
            setMinIterationTime(200)
            setTask(CollectUnusedImportsTask(this, dumbService, unusedImports, operationData))
        }
        progressManager.run(collectTask)
    }
}

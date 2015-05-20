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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetVisitor
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.flatMapDescendantsOfTypeVisitor
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.HashMap

public abstract class JetWholeProjectModalAction<D: Any>(val title: String) : IntentionAction {
    override final fun startInWriteAction() = false

    override final fun invoke(project: Project, editor: Editor?, file: PsiFile?) = invoke(project)

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    private fun invoke(project: Project) =
        ProgressManager.getInstance().run(
            object : Task.Modal(project, title, true) {
                override fun run(indicator: ProgressIndicator) {
                    val filesToData = HashMap<JetFile, D>()
                    runReadAction (fun() {
                        val files = PluginJetFilesProvider.allFilesInProject(project)

                        for ((i, currentFile) in files.withIndex()) {
                            indicator.setText("Checking file $i of ${files.size()}...")
                            indicator.setText2(currentFile.getVirtualFile().getPath())
                            indicator.setFraction((i + 1) / files.size().toDouble())
                            try {
                                val data = collectDataForFile(project, currentFile)
                                if (data != null) filesToData[currentFile] = data
                            }
                            catch (e: ProcessCanceledException) {
                                return
                            }
                            catch (e: Throwable) {
                                LOG.error(e)
                            }
                        }
                    })
                    applyAll(project, filesToData)
                }
            })

    private fun applyAll(project: Project, filesToData: Map<JetFile, D>) {
        UIUtil.invokeLaterIfNeeded {
            project.executeCommand(getText()) {
                runWriteAction {
                    filesToData.forEach {
                        try {
                            applyChangesForFile(project, it.getKey(), it.getValue())
                        }
                        catch (e: Throwable) {
                            LOG.error(e)
                        }
                    }
                }
            }
        }
    }

    // this method will be started under read action
    protected abstract fun collectDataForFile(project: Project, file: JetFile): D?

    // this method will be started under write action
    protected abstract fun applyChangesForFile(project: Project, file: JetFile, data: D)

    private companion object {
        val LOG = Logger.getInstance(javaClass<JetWholeProjectModalAction<*>>());
    }
}

public abstract class JetWholeProjectModalByCollectionAction<T : Any>(modalTitle: String)
: JetWholeProjectModalAction<Collection<T>>(modalTitle) {
    override fun collectDataForFile(project: Project, file: JetFile): Collection<T>? {
        val accumulator = arrayListOf<T>()
        collectTasksForFile(project, file, accumulator)
        return if (!accumulator.isEmpty()) return accumulator else null
    }

    abstract fun collectTasksForFile(project: Project, file: JetFile, accumulator: MutableCollection<T>)
}

class JetWholeProjectForEachElementOfTypeFix<T> private (
        private val collectingVisitorFactory: (MutableCollection<T>) -> JetVisitorVoid,
        private val tasksProcessor: (Collection<T>) -> Unit,
        private val name: String,
        private val familyName: String = name
) : JetWholeProjectModalByCollectionAction<T>("Applying '$name'") {

    override fun getFamilyName() = familyName
    override fun getText() = name

    override fun collectTasksForFile(project: Project, file: JetFile, accumulator: MutableCollection<T>) {
        file.accept(collectingVisitorFactory(accumulator))
    }
    override fun applyChangesForFile(project: Project, file: JetFile, data: Collection<T>) = tasksProcessor(data)

    companion object {
        inline fun <reified E : JetElement> createByPredicate(
                noinline predicate: (E) -> Boolean,
                noinline taskProcessor: (E) -> Unit,
                name: String,
                familyName: String = name
        ) = createByTaskFactory<E, E>(
                taskFactory = { if (predicate(it)) it else null },
                taskProcessor = taskProcessor,
                name = name,
                familyName = familyName
        )

        inline fun <reified E : JetElement, D : Any> createByTaskFactory(
                noinline taskFactory: (E) -> D?,
                noinline taskProcessor: (D) -> Unit,
                name: String,
                familyName: String = name
        ) = createForMultiTask<E, D>(
                tasksFactory = { taskFactory(it).singletonOrEmptyList() },
                tasksProcessor = { it.forEach(taskProcessor) },
                name = name,
                familyName = familyName
        )

        inline fun <reified E : JetElement, D> createForMultiTask(
                noinline tasksFactory: (E) -> Collection<D>,
                noinline tasksProcessor: (Collection<D>) -> Unit,
                name: String,
                familyName: String = name
        ) = JetWholeProjectForEachElementOfTypeFix(
                collectingVisitorFactory = { accumulator -> flatMapDescendantsOfTypeVisitor(accumulator, tasksFactory) },
                tasksProcessor = tasksProcessor,
                name = name,
                familyName = familyName
        )
    }
}

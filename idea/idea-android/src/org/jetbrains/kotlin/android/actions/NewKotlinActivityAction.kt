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

package org.jetbrains.kotlin.android.actions

import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.android.tools.idea.gradle.project.sync.GradleSyncState
import com.intellij.history.core.RevisionsCollector
import com.intellij.history.core.revisions.Revision
import com.intellij.history.integration.LocalHistoryImpl
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.util.messages.Topic
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.actions.JavaToKotlinAction
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import java.io.File
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberFunctions

private val NEW_KOTLIN_ACTIVITY_START_LABEL = "Start New Kotlin Activity Action"
private val NEW_KOTLIN_ACTIVITY_END_LABEL = "Finish New Kotlin Activity Action"

class NewKotlinActivityAction: AnAction(KotlinIcons.ACTIVITY) {

    companion object {
        internal fun attachGradleSyncListener(project: Project) {
            subscribe(project, gradleSyncListener)
        }

        internal fun willBeConvertedToKotlin(file: VirtualFile): Boolean {
            return javaFilesToKotlin?.any {
                it.virtualFile == file
            } ?: false
        }

        private val LOG = Logger.getInstance(NewKotlinActivityAction::class.java)
        private var javaFilesToKotlin: List<PsiJavaFile>? = null

        private fun convertFilesAfterProjectSync(files: List<PsiJavaFile>) {
            javaFilesToKotlin = files
        }

        private val gradleSyncListener = object: GradleSyncListener.Adapter() {
            override fun syncSucceeded(project: Project) {
                convertFiles(project)
            }

            override fun syncFailed(project: Project, errorMessage: String) {
                convertFiles(project)
            }

            private fun convertFiles(project: Project) {
                if (javaFilesToKotlin != null) {
                    DumbService.getInstance(project).smartInvokeLater {
                        val filesToConvert = javaFilesToKotlin!!.filter { it.isValid }
                        if (filesToConvert.isNotEmpty()) {
                            JavaToKotlinAction.convertFiles(filesToConvert, project, false)
                        }
                        javaFilesToKotlin = null
                    }
                }
            }
        }

        private fun subscribe(project: Project, listener: GradleSyncListener) {
            try {
                val subscribeFun = GradleSyncState::class.functions.find { it.name == "subscribe" && it.parameters.count() == 2 }
                if (subscribeFun != null) {
                    // AS 2.0
                    subscribeFun.call(project, listener)
                }
                else {
                    // AS 1.5
                    val connection = project.messageBus.connect(project)
                    val gradleSyncTopic = (GradleSyncState::class.java).getDeclaredField("GRADLE_SYNC_TOPIC").get(null) as Topic<GradleSyncListener>
                    connection.subscribe(gradleSyncTopic, gradleSyncListener)
                }
            }
            catch(e: Throwable) {
                LOG.error(e)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        LocalHistoryImpl.getInstanceImpl().putSystemLabel(project, NEW_KOTLIN_ACTIVITY_START_LABEL)
        val isSuccess = showWizard(e.dataContext)
        LocalHistoryImpl.getInstanceImpl().putSystemLabel(project, NEW_KOTLIN_ACTIVITY_END_LABEL)

        if (!isSuccess) return

        val localHistory = LocalHistoryImpl.getInstanceImpl()
        val gateway = localHistory.gateway!!
        val localHistoryFacade = localHistory.facade

        val revisionsCollector = RevisionsCollector(
                localHistoryFacade, gateway.createTransientRootEntry(),
                project.baseDir.path, project.locationHash, null)

        val revisions = revisionsCollector.result
        var endRevision: Revision? = null
        for (rev in revisions) {
            val label = rev.label ?: continue
            if (label == NEW_KOTLIN_ACTIVITY_END_LABEL) {
                endRevision = rev
            }

            if (label == NEW_KOTLIN_ACTIVITY_START_LABEL && endRevision != null) {
                val javaFiles = arrayListOf<PsiJavaFile>()
                val differences = endRevision.getDifferencesWith(rev)
                for (difference in differences) {
                    if (difference.right == null && difference.isFile) {
                        val file = File(difference.left.path)
                        if (file.extension == "java") {
                            val psiFile = file.toPsiFile(project) as? PsiJavaFile
                            if (psiFile != null) {
                                javaFiles.add(psiFile)
                            }
                        }
                    }
                }
                if (javaFiles.isNotEmpty()) {
                    val syncState = GradleSyncState.getInstance(project)
                    if (syncState.isSyncInProgress) {
                        convertFilesAfterProjectSync(javaFiles)
                    }
                    else {
                        JavaToKotlinAction.convertFiles(javaFiles, project, false)
                    }
                }
                break
            }
        }
    }

    private fun showWizard(dataContext: DataContext): Boolean {
        try {
            val wizardClass = try {
                // AS 1.5
                Class.forName("com.android.tools.idea.wizard.NewAndroidActivityWizard")
            }
            catch(e: ClassNotFoundException) {
                // AS 2.0
                Class.forName("com.android.tools.idea.npw.NewAndroidActivityWizard")
            }

            val constructor = wizardClass.getConstructor(Module::class.java, VirtualFile::class.java, File::class.java)
            val wizard = constructor.newInstance(
                    LangDataKeys.MODULE.getData(dataContext),
                    CommonDataKeys.VIRTUAL_FILE.getData(dataContext),
                    null)

            wizardClass.kotlin.functions.firstOrNull { it.name == "init" }?.call(wizard)
            return (wizardClass.kotlin.functions.firstOrNull { it.name == "showAndGet" }?.call(wizard) as? Boolean) ?: false
        }
        catch(e: Throwable) {
            LOG.error(e)
            return false
        }
    }

    override fun update(e: AnActionEvent) {
        val view = LangDataKeys.IDE_VIEW.getData(e.dataContext)
        val facet = getKotlinFacet(e)
        val presentation = e.presentation
        val isProjectReady = facet != null && isProjectReady(facet)
        presentation.text = "Kotlin Activity" + if (isProjectReady) "" else " (Project not ready)"
        presentation.isVisible = view != null && facet != null && isVisible(facet)
    }

    private fun getKotlinFacet(e: AnActionEvent): AndroidFacet? {
        val project = e.project
        if (project == null || project.isDisposed) return null
        val module = LangDataKeys.MODULE.getData(e.dataContext)
        return if (module != null) AndroidFacet.getInstance(module) else null
    }

    private fun isVisible(facet: AndroidFacet): Boolean {
        try {
            val shouldSetVisible = AndroidFacet::class.memberFunctions.singleOrNull {
                it.name == "isGradleProject" || it.name == "requiresAndroidModel"
            }

            return shouldSetVisible?.call(facet) != null
        }
        catch(e: Throwable) {
            LOG.error(e)
            return false
        }
    }

    private fun isProjectReady(facet: AndroidFacet): Boolean {
        try {
            val getAndroidProjectInfoFun = AndroidFacet::class.memberFunctions.singleOrNull {
                it.name == "getIdeaAndroidProject" || it.name == "getAndroidModel"
            }

            return getAndroidProjectInfoFun?.call(facet) != null
        }
        catch(e: Throwable) {
            LOG.error(e)
            return false
        }
    }

    override fun hashCode() = 0
    override fun equals(other: Any?) = other is NewKotlinActivityAction
}
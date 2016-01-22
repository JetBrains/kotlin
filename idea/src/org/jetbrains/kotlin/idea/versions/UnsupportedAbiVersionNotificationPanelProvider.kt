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

package org.jetbrains.kotlin.idea.versions

import com.intellij.ProjectTopics
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootAdapter
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import java.text.MessageFormat
import javax.swing.Icon

class UnsupportedAbiVersionNotificationPanelProvider(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootAdapter() {
            override fun rootsChanged(event: ModuleRootEvent?) {
                updateNotifications()
            }
        })
        connection.subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
            }

            override fun exitDumbMode() {
                updateNotifications()
            }
        })
    }

    private fun doCreate(module: Module, badRoots: Collection<VirtualFile>): EditorNotificationPanel {
        val answer = ErrorNotificationPanel()

        val kotlinLibraries = findAllUsedLibraries(project).keySet()
        val badRuntimeLibraries = kotlinLibraries.filter { library ->
            val runtimeJar = getLocalJar(JavaRuntimePresentationProvider.getRuntimeJar(library))
            val jsLibJar = getLocalJar(JSLibraryStdPresentationProvider.getJsStdLibJar(library))
            badRoots.contains(runtimeJar) || badRoots.contains(jsLibJar)
        }

        if (!badRuntimeLibraries.isEmpty()) {
            val otherBadRootsCount = badRoots.size - badRuntimeLibraries.size

            val text = MessageFormat.format("<html><b>{0,choice,0#|1#|1<Some }Kotlin runtime librar{0,choice,0#|1#y|1<ies}</b>" + "{1,choice,0#|1# and one other jar|1< and {1} other jars} " + "{1,choice,0#has|0<have} an unsupported format</html>",
                                            badRuntimeLibraries.size,
                                            otherBadRootsCount)

            val actionLabelText = MessageFormat.format("Update {0,choice,0#|1#|1<all }Kotlin runtime librar{0,choice,0#|1#y|1<ies} ",
                                                       badRuntimeLibraries.size)

            answer.setText(text)
            answer.createActionLabel(actionLabelText) { updateLibraries(project, badRuntimeLibraries) }
            if (otherBadRootsCount > 0) {
                createShowPathsActionLabel(module, answer, "Show all")
            }
        }
        else if (badRoots.size == 1) {
            val root = badRoots.iterator().next()
            val presentableName = root.presentableName
            answer.setText("<html>Kotlin library <b>'$presentableName'</b> has an unsupported format. Please update the library or the plugin</html>")

            answer.createActionLabel("Go to " + presentableName) { navigateToLibraryRoot(project, root) }
        }
        else {
            answer.setText("Some Kotlin libraries attached to this project have unsupported format. Please update the libraries or the plugin")
            createShowPathsActionLabel(module, answer, "Show paths")
        }
        return answer
    }

    private fun createShowPathsActionLabel(module: Module, answer: EditorNotificationPanel, labelText: String) {
        answer.createComponentActionLabel(labelText) { label ->
            DumbService.getInstance(project).tryRunReadActionInSmartMode({
                val badRoots = collectBadRoots(module)
                assert(!badRoots.isEmpty()) { "This action should only be called when bad roots are present" }

                val listPopupModel = LibraryRootsPopupModel("Unsupported format", project, badRoots)
                val popup = JBPopupFactory.getInstance().createListPopup(listPopupModel)
                popup.showUnderneathOf(label)

                null
            }, "Can't show all paths during index update")
        }
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        try {
            if (DumbService.isDumb(project)) return null
            if (ApplicationManager.getApplication().isUnitTestMode) return null
            if (file.fileType !== KotlinFileType.INSTANCE) return null

            if (CompilerManager.getInstance(project).isExcludedFromCompilation(file)) return null

            val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null

            return checkAndCreate(module)
        }
        catch (e: ProcessCanceledException) {
            // Ignore
        }
        catch (e: IndexNotReadyException) {
            DumbService.getInstance(project).runWhenSmart(updateNotifications)
        }

        return null
    }

    fun checkAndCreate(module: Module): EditorNotificationPanel? {
        val state = ServiceManager.getService(project, SuppressNotificationState::class.java).state
        if (state != null && state.isSuppressed) {
            return null
        }

        val badRoots = collectBadRoots(module)
        if (!badRoots.isEmpty()) {
            return doCreate(module, badRoots)
        }

        return null
    }

    private class LibraryRootsPopupModel(title: String, private val project: Project, roots: Collection<VirtualFile>)
        : BaseListPopupStep<VirtualFile>(title, *roots.toTypedArray()) {

        override fun getTextFor(root: VirtualFile): String {
            val relativePath = VfsUtilCore.getRelativePath(root, project.baseDir, '/')
            return relativePath ?: root.path
        }

        override fun getIconFor(aValue: VirtualFile): Icon? {
            if (aValue.isDirectory) {
                return AllIcons.Nodes.Folder
            }
            return AllIcons.FileTypes.Archive
        }

        override fun onChosen(selectedValue: VirtualFile, finalChoice: Boolean): PopupStep<Any>? {
            navigateToLibraryRoot(project, selectedValue)
            return PopupStep.FINAL_CHOICE
        }

        override fun isSpeedSearchEnabled(): Boolean = true
    }

    private class ErrorNotificationPanel : EditorNotificationPanel() {
        init {
            myLabel.icon = AllIcons.General.Error
        }
    }

    private val updateNotifications = Runnable { updateNotifications() }

    private fun updateNotifications() {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("unsupported.abi.version")

        private fun navigateToLibraryRoot(project: Project, root: VirtualFile) {
            OpenFileDescriptor(project, root).navigate(true)
        }

        fun collectBadRoots(module: Module): Collection<VirtualFile> {
            val badJVMRoots = getLibraryRootsWithAbiIncompatibleKotlinClasses(module)
            val badJSRoots = getLibraryRootsWithAbiIncompatibleForKotlinJs(module)

            if (badJVMRoots.isEmpty() && badJSRoots.isEmpty()) return emptyList()

            return (badJVMRoots + badJSRoots).toHashSet()
        }
    }
}

fun EditorNotificationPanel.createComponentActionLabel(labelText: String, callback: (HyperlinkLabel) -> Unit) {
    val label: Ref<HyperlinkLabel> = Ref.create()
    val action = Runnable {
        callback(label.get())
    }
    label.set(createActionLabel(labelText, action))
}

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.text.MessageFormat
import java.util.*
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.event.HyperlinkEvent

class UnsupportedAbiVersionNotificationPanelProvider(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
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

    private fun doCreate(module: Module, badVersionedRoots: Collection<BinaryVersionedFile<BinaryVersion>>): EditorNotificationPanel {
        val answer = ErrorNotificationPanel()
        val badRootFiles = badVersionedRoots.map { it.file }

        val kotlinLibraries = findAllUsedLibraries(project).keySet()
        val badRuntimeLibraries = kotlinLibraries.filter { library ->
            val runtimeJar = getLocalJar(LibraryJarDescriptor.RUNTIME_JAR.findExistingJar(library))
            val jsLibJar = getLocalJar(LibraryJarDescriptor.JS_STDLIB_JAR.findExistingJar(library))
            badRootFiles.contains(runtimeJar) || badRootFiles.contains(jsLibJar)
        }

        val isPluginOldForAllRoots = badVersionedRoots.all { it.supportedVersion < it.version }
        val isPluginNewForAllRoots = badVersionedRoots.all { it.supportedVersion > it.version }

        if (!badRuntimeLibraries.isEmpty()) {
            val badRootsInRuntimeLibraries = findBadRootsInRuntimeLibraries(badRuntimeLibraries, badVersionedRoots)
            val otherBadRootsCount = badVersionedRoots.size - badRootsInRuntimeLibraries.size

            val text = MessageFormat.format("<html><b>{0,choice,0#|1#|1<Some }Kotlin runtime librar{0,choice,0#|1#y|1<ies}</b>" +
                                            "{1,choice,0#|1# and one other jar|1< and {1} other jars} " +
                                            "{1,choice,0#has|0<have} an unsupported binary format.</html>",
                                            badRuntimeLibraries.size,
                                            otherBadRootsCount)

            answer.setText(text)

            if (isPluginOldForAllRoots) {
                createUpdatePluginLink(answer)
            }

            val isPluginOldForAllRuntimeLibraries = badRootsInRuntimeLibraries.all { it.supportedVersion < it.version }
            val isPluginNewForAllRuntimeLibraries = badRootsInRuntimeLibraries.all { it.supportedVersion > it.version }

            val updateAction = when {
                isPluginNewForAllRuntimeLibraries -> "Update"
                isPluginOldForAllRuntimeLibraries -> "Downgrade"
                else -> "Replace"
            }

            val actionLabelText = MessageFormat.format("$updateAction {0,choice,0#|1#|1<all }Kotlin runtime librar{0,choice,0#|1#y|1<ies} ", badRuntimeLibraries.size)
            answer.createActionLabel(actionLabelText) {
                ApplicationManager.getApplication().invokeLater {
                    updateLibraries(project, badRuntimeLibraries)
                }
            }
        }
        else if (badVersionedRoots.size == 1) {
            val badVersionedRoot = badVersionedRoots.first()
            val presentableName = badVersionedRoot.file.presentableName

            when {
                isPluginOldForAllRoots -> {
                    answer.setText("<html>Kotlin library <b>'$presentableName'</b> was compiled with a newer Kotlin compiler and can't be read. Please update Kotlin plugin.</html>")
                    createUpdatePluginLink(answer)
                }

                isPluginNewForAllRoots ->
                    answer.setText("<html>Kotlin library <b>'$presentableName'</b> has outdated binary format and can't be read by current plugin. Please update the library.</html>")

                else -> {
                    throw IllegalStateException("Bad root with compatible version found: $badVersionedRoot")
                }
            }

            answer.createActionLabel("Go to " + presentableName) { navigateToLibraryRoot(project, badVersionedRoot.file) }
        }
        else {
            when {
                isPluginOldForAllRoots -> {
                    answer.setText("Some Kotlin libraries attached to this project were compiled with a newer Kotlin compiler and can't be read. " +
                                   "Please update Kotlin plugin.")
                    createUpdatePluginLink(answer)
                }

                isPluginNewForAllRoots ->
                    answer.setText("Some Kotlin libraries attached to this project have outdated binary format and can't be read by current plugin. " +
                                   "Please update found libraries.")

                else ->
                    answer.setText("Some Kotlin libraries attached to this project have unsupported binary format. Please update the libraries or the plugin.")
            }
        }

        createShowPathsActionLabel(module, answer, "Details")

        return answer
    }

    private fun createShowPathsActionLabel(module: Module, answer: EditorNotificationPanel, labelText: String) {
        answer.createComponentActionLabel(labelText) { label ->
            DumbService.getInstance(project).tryRunReadActionInSmartMode({
                val badRoots = collectBadRoots(module)
                assert(!badRoots.isEmpty()) { "This action should only be called when bad roots are present" }

                val listPopupModel = LibraryRootsPopupModel("Unsupported format, plugin version: " + KotlinPluginUtil.getPluginVersion(), project, badRoots)
                val popup = JBPopupFactory.getInstance().createListPopup(listPopupModel)
                popup.showUnderneathOf(label)

                null
            }, "Can't show all paths during index update")
        }
    }

    private fun createUpdatePluginLink(answer: ErrorNotificationPanel) {
        answer.createProgressAction("     Check...", "Update plugin") { link, updateLink ->
            KotlinPluginUpdater.getInstance().runCachedUpdate { pluginUpdateStatus ->
                when (pluginUpdateStatus) {
                    is PluginUpdateStatus.Update -> {
                        link.isVisible = false
                        updateLink.isVisible = true

                        updateLink.addHyperlinkListener(object : HyperlinkAdapter() {
                            override fun hyperlinkActivated(e: HyperlinkEvent) {
                                KotlinPluginUpdater.getInstance().installPluginUpdate(pluginUpdateStatus)
                            }
                        })
                    }
                    is PluginUpdateStatus.LatestVersionInstalled -> {
                        link.text = "No updates found"
                    }
                }

                false  // do not auto-retry update check
            }
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
        if (state.isSuppressed) {
            return null
        }

        val badRoots = collectBadRoots(module)
        if (!badRoots.isEmpty()) {
            return doCreate(module, badRoots)
        }

        return null
    }

    private fun findBadRootsInRuntimeLibraries(
            badRuntimeLibraries: List<Library>,
            badVersionedRoots: Collection<BinaryVersionedFile<BinaryVersion>>): ArrayList<BinaryVersionedFile<BinaryVersion>> {
        val badRootsInLibraries = ArrayList<BinaryVersionedFile<BinaryVersion>>()

        fun addToBadRoots(file: VirtualFile?) {
            if (file != null) {
                val runtimeJarBadRoot = badVersionedRoots.firstOrNull { it.file == file }
                if (runtimeJarBadRoot != null) {
                    badRootsInLibraries.add(runtimeJarBadRoot)
                }
            }
        }

        badRuntimeLibraries.forEach { library ->
            for (descriptor in LibraryJarDescriptor.values()) {
                addToBadRoots(getLocalJar(descriptor.findExistingJar(library)))
            }
        }

        return badRootsInLibraries
    }

    private class LibraryRootsPopupModel(
            title: String,
            private val project: Project,
            roots: Collection<BinaryVersionedFile<BinaryVersion>>
    ) : BaseListPopupStep<BinaryVersionedFile<BinaryVersion>>(title, *roots.toTypedArray()) {

        override fun getTextFor(root: BinaryVersionedFile<BinaryVersion>): String {
            val relativePath = VfsUtilCore.getRelativePath(root.file, project.baseDir, '/')
            return "${relativePath ?: root.file.path} (${root.version}) - expected: ${root.supportedVersion}"
        }

        override fun getIconFor(aValue: BinaryVersionedFile<BinaryVersion>): Icon? {
            if (aValue.file.isDirectory) {
                return AllIcons.Nodes.Folder
            }
            return AllIcons.FileTypes.Archive
        }

        override fun onChosen(selectedValue: BinaryVersionedFile<BinaryVersion>, finalChoice: Boolean): PopupStep<Any>? {
            navigateToLibraryRoot(project, selectedValue.file)
            return PopupStep.FINAL_CHOICE
        }

        override fun isSpeedSearchEnabled(): Boolean = true
    }

    private class ErrorNotificationPanel : EditorNotificationPanel() {
        init {
            myLabel.icon = AllIcons.General.Error
        }

        fun createProgressAction(text: String, successLinkText: String, updater: (JLabel, HyperlinkLabel) -> Unit) {
            val label = JLabel(text)
            myLinksPanel.add(label)

            val successLink = createActionLabel(successLinkText, {  })
            successLink.isVisible = false

            // Several notification panels can be created almost instantly but we want to postpone deferred checks until
            // panels are actually visible on screen.
            myLinksPanel.addComponentListener(object : ComponentAdapter() {
                var isUpdaterCalled = false
                override fun componentResized(p0: ComponentEvent?) {
                    if (!isUpdaterCalled) {
                        isUpdaterCalled = true
                        updater(label, successLink)
                    }
                }
            })
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

        fun collectBadRoots(module: Module): Collection<BinaryVersionedFile<BinaryVersion>> {
            val badRoots = when (TargetPlatformDetector.getPlatform(module)) {
                JvmPlatform -> getLibraryRootsWithAbiIncompatibleKotlinClasses(module)
                JsPlatform -> getLibraryRootsWithAbiIncompatibleForKotlinJs(module)
                else -> return emptyList()
            }

            return if (badRoots.isEmpty()) emptyList() else badRoots.toHashSet()
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

private operator fun BinaryVersion.compareTo(other: BinaryVersion): Int {
    val first = this.toArray()
    val second = other.toArray()
    for (i in 0 until maxOf(first.size, second.size)) {
        val thisPart = first.getOrNull(i) ?: -1
        val otherPart = second.getOrNull(i) ?: -1

        if (thisPart != otherPart) {
            return thisPart - otherPart
        }
    }

    return 0
}

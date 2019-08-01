/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.isGradleModule
import org.jetbrains.kotlin.idea.util.findModule
import org.jetbrains.kotlin.idea.util.sourceRoots

class JavaOutsideModuleDetector(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>(), StartupActivity {

    override fun runActivity(project: Project) {
        val notifications = EditorNotifications.getInstance(project)
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileMoved(event: VirtualFileMoveEvent) {
                if (event.file.fileType == JavaFileType.INSTANCE) notifications.updateNotifications(event.file)
            }
        }, project)
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (file.fileType != JavaFileType.INSTANCE) return null
        val module = file.findModule(project) ?: return null
        if (!module.isGradleModule()) return null
        val facetSettings = KotlinFacet.get(module)?.configuration?.settings ?: return null

        val filePath = file.path
        val nonKotlinPath = module.sourceRoots.map { it.path } - facetSettings.pureKotlinSourceFolders
        if (nonKotlinPath.any { filePath.startsWith(it) }) return null
        return EditorNotificationPanel().apply {
            text("This .java file is outside of Java source roots and won't be added to the classpath.")
            icon(AllIcons.General.Warning)
        }
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("JavaOutsideModuleDetector")
    }
}
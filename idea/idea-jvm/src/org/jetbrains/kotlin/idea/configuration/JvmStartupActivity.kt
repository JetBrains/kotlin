/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ProjectTopics
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.ui.EditorNotifications

class JvmStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        })

        connection.subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
            }

            override fun exitDumbMode() {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        })

        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileMoved(event: VirtualFileMoveEvent) {
                if (event.file.fileType == JavaFileType.INSTANCE) EditorNotifications.getInstance(project).updateNotifications(event.file)
            }
        }, project)
    }
}
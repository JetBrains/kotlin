/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.cidr.apple.gradle

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.mobile.MobileBundle
import java.io.File

class AppleXcodeProjectFileModificationListener(private val project: Project, private val projectFile: File) : AsyncFileListener {
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val changed = events.any {
            val file = it.file
            file != null && it is VFileContentChangeEvent && VfsUtilCore.virtualToIoFile(file) == projectFile
        }

        if (changed) {
            showWarning()
        }
        return null
    }

    @Synchronized
    private fun showWarning() {
        val currentTime = System.currentTimeMillis()
        val tooSoon = (currentTime - lastNotified) < 2_000
        if (tooSoon) return

        Notifications.Bus.notify(
            Notification(
                MobileBundle.message("notification.group.id"),
                MobileBundle.message("xcodeproj.changed.manually"),
                MobileBundle.message("xcodeproj.changed.manually.description"),
                NotificationType.WARNING
            ), project
        )
        lastNotified = currentTime
    }

    private var lastNotified = 0L

    companion object {
        fun setUp(project: Project, target: AppleTargetModel, disposable: Disposable) {
            val pbxProjFile = AppleXcodeProjectFileProvider.findXcodeProjFile(target).resolve("project.pbxproj")
            VfsUtil.findFileByIoFile(pbxProjFile, true)

            VirtualFileManager.getInstance().addAsyncFileListener(
                AppleXcodeProjectFileModificationListener(project, pbxProjFile),
                disposable
            )
        }
    }
}
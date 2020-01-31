/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.cidr.apple.gradle

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.FutureResult
import com.jetbrains.cidr.xcode.XcodeProjectFileProvider
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID

class AppleXcodeProjectFileProvider : XcodeProjectFileProvider {
    override val isImmediate: Boolean = false

    override fun findXcodeProjFile(project: Project, file: VirtualFile): VirtualFile? {
        val success = generateXcodeProject(project)
        if (!success) return null

        val targets = GradleAppleWorkspace.getInstance(project).targets
        val ioFile = VfsUtil.virtualToIoFile(file)
        val target = targets.find { targetModel ->
            targetModel.sourceFolders.any { FileUtil.isAncestor(it, ioFile, false) }
            // TODO find more efficient way, maybe with ModuleFileIndex
        } ?: return null

        val xcodeProjFile = target.buildDir.resolve("${target.name}.xcodeproj")
        return VfsUtil.findFileByIoFile(xcodeProjFile, true)
    }

    companion object {
        @Synchronized
        private fun generateXcodeProject(project: Project): Boolean {
            val settings = ExternalSystemTaskExecutionSettings()
            settings.externalSystemIdString = GRADLE_SYSTEM_ID.id
            settings.externalProjectPath = project.basePath
            settings.executionName = MobileBundle.message("generate.xcodeproj")
            settings.taskNames = listOf("generateXcodeproj")

            val success = FutureResult<Boolean>()
            val callback = object : TaskCallback {
                override fun onSuccess() {
                    success.set(true)
                }

                override fun onFailure() {
                    success.set(false)
                }
            }

            ExternalSystemUtil.runTask(
                settings, DefaultRunExecutor.EXECUTOR_ID, project, GRADLE_SYSTEM_ID,
                callback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false
            )

            return success.get()
        }
    }
}
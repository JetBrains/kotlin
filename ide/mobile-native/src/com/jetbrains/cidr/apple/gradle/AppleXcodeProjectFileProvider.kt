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
import com.jetbrains.cidr.xcode.model.PBXProjectFile
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import java.io.File

class AppleXcodeProjectFileProvider : XcodeProjectFileProvider {
    override val isImmediate: Boolean = false

    override fun findXcodeProjFile(project: Project, file: VirtualFile): File? {
        val targets = GradleAppleWorkspace.getInstance(project).targets
        val ioFile = VfsUtil.virtualToIoFile(file)
        val target = targets.find { targetModel ->
            targetModel.sourceFolders.any { FileUtil.isAncestor(it, ioFile, false) }
            // TODO find more efficient way, maybe with ModuleFileIndex
        } ?: return null

        return generateXcodeProject(project, target)
    }

    companion object {
        fun findXcodeProjFile(target: AppleTargetModel): File =
            target.editableXcodeProjectDir.resolve("${target.name}.xcodeproj")

        @Synchronized
        private fun generateXcodeProject(project: Project, target: AppleTargetModel): File? {
            val xcodeProjFile = findXcodeProjFile(target)

            val settings = ExternalSystemTaskExecutionSettings()
            settings.externalSystemIdString = GRADLE_SYSTEM_ID.id
            settings.externalProjectPath = project.basePath
            settings.executionName = MobileBundle.message("generate.xcodeproj")
            settings.taskNames = listOf("generateXcodeproj")
            settings.vmOptions = "-DxcodeProjBaseDir='${xcodeProjFile.parent}'"

            val success = FutureResult<File?>()
            val callback = object : TaskCallback {
                override fun onSuccess() {
                    VfsUtil.findFileByIoFile(xcodeProjFile.resolve(PBXProjectFile.PROJECT_FILE), true)
                    success.set(xcodeProjFile)
                }

                override fun onFailure() {
                    success.set(null)
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
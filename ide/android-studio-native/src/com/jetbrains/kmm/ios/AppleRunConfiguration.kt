/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.ComponentUtil
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.testing.CidrLauncher
import com.jetbrains.konan.KonanBundle
import com.jetbrains.konan.WorkspaceXML
import com.jetbrains.mobile.execution.*
import org.jdom.Element
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AppleRunConfiguration(project: Project, configurationFactory: AppleConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name),
    MobileRunConfiguration {
    private val workspace = ProjectWorkspace.getInstance(project)

    val iosBuildDirectory = "build/ios" // this directory is removed by Gradle clean command

    val xcProjectFile get() = workspace.xcProjectFile
    private val xcodeSchemeLock = ReentrantLock()
    var xcodeScheme: String? = null
        get() = xcodeSchemeLock.withLock {
            if (field == null) {
                // initially we pick scheme with the name of the project or first one
                val schemes = xcProjectFile?.schemes ?: emptyList()
                field = if (xcProjectFile?.projectName in schemes) {
                    xcProjectFile?.projectName
                } else {
                    schemes.firstOrNull()
                }
            }
            return field
        }
        set(value) = xcodeSchemeLock.withLock {
            if (value != null) {
                field = value
            }
        }

    fun xcodeSdk(target: ExecutionTarget): String =
        if (target is ApplePhysicalDevice) "iphoneos" else "iphonesimulator"

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        AppleRunConfigurationEditor(project)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> =
        mutableListOf(BuildIOSAppTask())

    override fun checkConfiguration() {
        val propertyKey = KonanBundle.message("property.xcodeproj")
        val status = workspace.xcProjectStatus

        when {
            status is XcProjectStatus.Misconfiguration -> throwConfigurationError(
                "Project is misconfigured: " + status.reason
            )
            status == XcProjectStatus.NotLocated -> throwConfigurationError(
                "Please specify Xcode project location in $propertyKey property of gradle.properties",
                Runnable {
                    fixXcProjectPath()
                    openGradlePropertiesFile()
                    closeSettingsDialog()
                }
            )
            status is XcProjectStatus.NotFound -> throwConfigurationError(
                "Please check $propertyKey property of gradle.properties: " + status.reason,
                Runnable {
                    openGradlePropertiesFile()
                    closeSettingsDialog()
                }
            )
            xcProjectFile!!.schemes.isEmpty() -> throwConfigurationError(
                "Please check specified Xcode project file: " + xcProjectFile!!.schemesStatus
            )
            xcodeScheme == null -> throwConfigurationError(
                "Please select Xcode scheme"
            )
            xcodeScheme!! !in xcProjectFile!!.schemes -> throwConfigurationError(
                "Selected scheme '$xcodeScheme' is not found in ${xcProjectFile!!.absolutePath}"
            )
        }
    }

    private fun throwConfigurationError(
        message: String,
        quickFix: Runnable? = null
    ) {
        throw RuntimeConfigurationError(message).apply {
            this.quickFix = quickFix
        }
    }

    private fun fixXcProjectPath() {
        findNearestXcProject()?.let { xcFile ->
            val propFile = File(project.basePath, "gradle.properties")
            LocalFileSystem.getInstance().findFileByIoFile(propFile)?.let { vf ->
                WriteCommandAction.runWriteCommandAction(project) {
                    val text = VfsUtilCore.loadText(vf) + "\nxcodeproj=${xcFile.relativeTo(propFile.parentFile)}"
                    VfsUtil.saveText(vf, text)
                }
            }
        }
    }

    private fun findNearestXcProject(): File? =
        File(project.basePath).walk().maxDepth(2).firstOrNull { f ->
            f.extension == XcFileExtensions.project || f.extension == XcFileExtensions.workspace
        }

    private fun openGradlePropertiesFile() {
        val propFile = File(project.basePath, "gradle.properties")
        LocalFileSystem.getInstance().findFileByIoFile(propFile)?.let { vf ->
            OpenFileAction.openFile(vf, project)
        }
    }

    private fun closeSettingsDialog() {
        (ComponentUtil.getActiveWindow() as? DialogWrapperDialog)
            ?.dialogWrapper
            ?.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    override fun canRunOn(target: ExecutionTarget): Boolean = target is Device

    override fun getProductBundle(environment: ExecutionEnvironment): File {
        val buildType = if (environment.executionTarget is ApplePhysicalDevice) "Debug-iphoneos" else "Debug-iphonesimulator"
        if (project.basePath == null) throw RuntimeConfigurationError("Can't run ${this::class.simpleName} on project without base path.")
        return File(project.basePath).resolve(iosBuildDirectory).resolve("$buildType/$xcodeScheme.app")
    }

    override fun createOtherState(environment: ExecutionEnvironment): CommandLineState {
        throw IllegalStateException()
    }

    override fun createLauncher(environment: ExecutionEnvironment): CidrLauncher =
        object : AppleLauncher<AppleRunConfiguration>(this, environment, environment.executionTarget as AppleDevice) {
            override fun createDebuggerDriverConfiguration(): DebuggerDriverConfiguration =
                AppleLLDBDriverConfiguration()
        }

    override fun readExternal(element: Element) {
        super<LocatableConfigurationBase>.readExternal(element)
        xcodeScheme = element.getAttributeValue(WorkspaceXML.RunConfiguration.attributeXcodeScheme)
    }

    override fun writeExternal(element: Element) {
        super<LocatableConfigurationBase>.writeExternal(element)
        if (xcodeScheme != null) {
            element.setAttribute(WorkspaceXML.RunConfiguration.attributeXcodeScheme, xcodeScheme)
        }
    }
}

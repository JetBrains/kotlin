/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ComponentUtil
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.testing.CidrLauncher
import com.jetbrains.kmm.KmmBundle
import com.jetbrains.kmm.ios.execution.AppleRunner.Companion.bundledBindingsPath
import com.jetbrains.mobile.execution.*
import com.jetbrains.mpp.loadPythonBindings
import org.jdom.Element
import java.io.File

class AppleRunConfiguration(project: Project, configurationFactory: AppleConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name),
    MobileRunConfiguration {
    private val workspace = ProjectWorkspace.getInstance(project)
    private val projectPath: String
        get() = project.basePath ?: throw RuntimeConfigurationError("Can't work with project without base path.")

    val iosBuildDirectory = "build/ios" // this directory is removed by Gradle clean command

    var executionTarget: AppleDevice =
        DeviceService.getInstance(project).getIosDevices().first()

    val xcodeSdk: String
        get() = if (executionTarget is ApplePhysicalDevice) "iphoneos" else "iphonesimulator"

    val xcProjectFile get() = workspace.xcProjectFile

    var xcodeScheme: String? = null
        get() {
            if (field == null) {
                field = xcProjectFile?.schemes?.let { schemes ->
                    // initially we pick scheme with the name of the project or first one
                    schemes.firstOrNull { it == xcProjectFile?.projectName } ?: schemes.firstOrNull()
                }
            }
            return field
        }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        AppleRunConfigurationEditor(project)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> =
        mutableListOf(BuildIOSAppTask())

    override fun checkConfiguration() {
        val propertyKey = XcProjectFile.gradleProperty
        val status = workspace.xcProjectStatus

        when {
            status is XcProjectStatus.Misconfiguration -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.misconfigured", status.reason)
            )
            status == XcProjectStatus.NotLocated -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.xcodeNotLocated", propertyKey),
                Runnable {
                    XcProjectFile.findXcFile(File(projectPath))?.let {
                        XcProjectFile.setupXcProjectPath(project, it)
                    }
                    openGradlePropertiesFile()
                    closeSettingsDialog()
                }
            )
            status is XcProjectStatus.NotFound -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.xcodeNotFound", propertyKey, status.reason),
                Runnable {
                    openGradlePropertiesFile()
                    closeSettingsDialog()
                }
            )
            xcProjectFile!!.schemes.isEmpty() -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.xcodeEmptySchemes", xcProjectFile!!.schemesStatus)
            )
            xcodeScheme == null -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.xcodeNullScheme")
            )
            xcodeScheme!! !in xcProjectFile!!.schemes -> throwConfigurationError(
                KmmBundle.message("apple.runconfig.error.xcodeInvalidScheme", xcodeScheme!!, xcProjectFile!!.absolutePath)
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

    private fun openGradlePropertiesFile() {
        val propFile = File(projectPath, "gradle.properties")
        LocalFileSystem.getInstance().findFileByIoFile(propFile)?.let { vf ->
            OpenFileAction.openFile(vf, project)
        }
    }

    private fun closeSettingsDialog() {
        (ComponentUtil.getActiveWindow() as? DialogWrapperDialog)
            ?.dialogWrapper
            ?.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    override fun getProductBundle(device: Device): File {
        val buildType = if (executionTarget is ApplePhysicalDevice) "Debug-iphoneos" else "Debug-iphonesimulator"
        return File(projectPath).resolve(iosBuildDirectory).resolve("$buildType/$xcodeScheme.app")
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState =
        when (executionTarget) {
            is AppleDevice -> createAppleState(environment, executor, executionTarget)
            else -> throw IllegalStateException()
        }

    override fun createLauncher(environment: ExecutionEnvironment, device: AppleDevice): CidrLauncher =
        object : AppleLauncher<AppleRunConfiguration>(this, environment, executionTarget) {
            override fun createDebuggerDriverConfiguration() = AppleLLDBDriverConfiguration()

            override fun configureDebugProcess(process: CidrDebugProcess) {
                process.loadPythonBindings(bundledBindingsPath)
            }
        }

    override fun readExternal(element: Element) {
        super<LocatableConfigurationBase>.readExternal(element)
        xcodeScheme = element.getAttributeValue(attributeXcodeScheme)

        element.getAttributeValue(attributeExecutionTargetId)?.let { deviceId ->
            DeviceService.getInstance(project).getIosDevices()
                .firstOrNull { it.id == deviceId }
                ?.let { executionTarget = it }
        }
    }

    override fun writeExternal(element: Element) {
        super<LocatableConfigurationBase>.writeExternal(element)
        xcodeScheme?.let { element.setAttribute(attributeXcodeScheme, it) }
        element.setAttribute(attributeExecutionTargetId, executionTarget.id)
    }

    companion object {
        private const val attributeXcodeScheme = "XCODE_SCHEME"
        private const val attributeExecutionTargetId = "EXEC_TARGET_ID"
    }
}

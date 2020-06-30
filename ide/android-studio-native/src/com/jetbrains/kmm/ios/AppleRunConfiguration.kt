/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.vfs.LocalFileSystem
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
    LocatableConfigurationBase<Element>(project, configurationFactory, name), RunConfigurationWithSuppressedDefaultRunAction,
    MobileRunConfiguration {
    val workspace = ProjectWorkspace.getInstance(project)

    private val xcodeSchemeLock = ReentrantLock()
    var xcodeScheme: String? = null
        get() = xcodeSchemeLock.withLock {
            if (field == null) {
                // initially we pick scheme with the name of the project or first one
                val schemes = workspace.xcProjectFile?.schemes ?: emptyList()
                field = if (workspace.xcProjectFile?.projectName in schemes) {
                    workspace.xcProjectFile?.projectName
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

    fun xcodeSdk(target: ExecutionTarget): String {
        return if (target is ApplePhysicalDevice) "iphoneos" else "iphonesimulator"
    }

    val iosBuildDirectory = "build/ios" // this directory is removed by Gradle clean command

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        AppleRunConfigurationEditor(project)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
        val result = mutableListOf<BeforeRunTask<*>>()
        result.add(BuildIOSAppTask())
        return result
    }

    private val openGradleProperties = Runnable {
        val propertiesFile = LocalFileSystem.getInstance().findFileByIoFile(File(project.basePath, "gradle.properties"))

        if (propertiesFile != null) {
            OpenFileAction.openFile(propertiesFile, project)
        }

        val runConfigurationsSettingsWindow = (ComponentUtil.getActiveWindow() as? DialogWrapperDialog) ?: return@Runnable
        runConfigurationsSettingsWindow.dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    private fun reportXcFileError(message: String, quickFix: Runnable? = null) {
        throw if (quickFix == null)
            RuntimeConfigurationError(message)
        else
            RuntimeConfigurationError(message, quickFix)
    }

    override fun checkConfiguration() {
        val propertyKey = KonanBundle.message("property.xcodeproj")

        when (val status = workspace.xcProjectStatus) {
            is XcProjectStatus.Misconfiguration ->
                reportXcFileError("Project is misconfigured: " + status.reason)
            XcProjectStatus.NotLocated ->
                reportXcFileError(
                    "Please specify Xcode project location in" +
                            " $propertyKey property of gradle.properties",
                    openGradleProperties
                )
            is XcProjectStatus.NotFound ->
                reportXcFileError(
                    "Please check $propertyKey property of gradle.properties: " + status.reason,
                    openGradleProperties
                )
        }

        if (workspace.xcProjectFile!!.schemes.isEmpty()) {
            throw RuntimeConfigurationError("Please check specified Xcode project file: " + workspace.xcProjectFile!!.schemesStatus)
        }

        if (xcodeScheme == null) {
            throw RuntimeConfigurationError("Please select Xcode scheme")
        }

        if (xcodeScheme !in workspace.xcProjectFile!!.schemes) {
            throw RuntimeConfigurationError("Selected scheme '$xcodeScheme' is not found in ${workspace.xcProjectFile!!.absolutePath}")
        }
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

    override fun createCidrLauncher(environment: ExecutionEnvironment, device: AppleDevice): CidrLauncher =
        object : AppleLauncher<AppleRunConfiguration>(this, environment, device) {
            override fun createDebuggerDriverConfiguration(): DebuggerDriverConfiguration =
                AppleLLDBDriverConfiguration()
        }

    var selectedDevice: Device? = null

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

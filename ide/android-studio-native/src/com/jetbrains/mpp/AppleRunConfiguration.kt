/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.kmm.AppleConfigurationFactory
import com.jetbrains.kmm.AppleRunConfigurationEditor
import com.jetbrains.konan.KonanBundle
import com.jetbrains.konan.WorkspaceXML
import com.jetbrains.mpp.execution.ApplePhysicalDevice
import com.jetbrains.mpp.execution.Device
import org.jdom.Element
import java.io.File

class AppleRunConfiguration(project: Project, configurationFactory: AppleConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name), RunConfigurationWithSuppressedDefaultRunAction {

    val workspace = ProjectWorkspace.getInstance(project)

    private var _xcodeScheme: String? = null
    var xcodeScheme: String?
        get() {
            if (_xcodeScheme == null) {
                // initially we pick scheme with the name of the project or first one
                val schemes = workspace.xcProjectFile?.schemes ?: emptyList()
                _xcodeScheme = if (workspace.xcProjectFile?.projectName in schemes) {
                    workspace.xcProjectFile?.projectName
                } else {
                    schemes.firstOrNull()
                }
            }
            return _xcodeScheme
        }
        set(value) {
            if (value != null) {
                _xcodeScheme = value
            }
        }

    fun xcodeSdk(target: ExecutionTarget): String {
        return if (target is ApplePhysicalDevice) "iphoneos" else "iphonesimulator"
    }

    val iosBuildDirectory = "ios_build" // TODO: Allow configuration.

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        AppleRunConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        (environment.executionTarget as? Device)?.createState(this, environment)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
        val result = mutableListOf<BeforeRunTask<*>>()
        result.add(BuildIOSAppTask())
        return result
    }

    override fun checkConfiguration() {
        val propertyKey = KonanBundle.message("property.xcodeproj")

        val projectFileError = when (val status = workspace.xcProjectStatus) {
            is XCProjectStatus.Misconfiguration ->
                "Project is misconfigured: " + status.reason
            XCProjectStatus.NotLocated ->
                "Please specify Xcode project location path relative to root in $propertyKey property of gradle.properties"
            is XCProjectStatus.NotFound ->
                "Please check $propertyKey property of gradle.properties: " + status.reason
            XCProjectStatus.Found -> ""
        }

        if (projectFileError.isNotEmpty()) {
            throw RuntimeConfigurationError(projectFileError)
        }

        if (workspace.xcProjectFile!!.schemes.isEmpty()) {
            throw RuntimeConfigurationError("Pleases check specified Xcode project file: " + workspace.xcProjectFile!!.schemesStatus)
        }

        if (xcodeScheme == null) {
            throw RuntimeConfigurationError("Pleases select Xcode scheme")
        }

        if (xcodeScheme !in workspace.xcProjectFile!!.schemes) {
            throw RuntimeConfigurationError("Selected scheme '$xcodeScheme' is not found in ${workspace.xcProjectFile!!.absolutePath}")
        }
    }

    override fun canRunOn(target: ExecutionTarget): Boolean = target is Device

    fun getProductBundle(environment: ExecutionEnvironment): File {
        val buildType = if (environment.executionTarget is ApplePhysicalDevice) "Debug-iphoneos" else "Debug-iphonesimulator"
        if (project.basePath == null) throw RuntimeConfigurationError("Can't run ${this::class.simpleName} on project without base path.")
        return File(project.basePath).resolve(iosBuildDirectory).resolve("$buildType/$xcodeScheme.app")
    }

    var selectedDevice: Device? = null

    override fun readExternal(element: Element) {
        super.readExternal(element)
        xcodeScheme = element.getAttributeValue(WorkspaceXML.RunConfiguration.attributeXcodeScheme)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        if (xcodeScheme != null) {
            element.setAttribute(WorkspaceXML.RunConfiguration.attributeXcodeScheme, xcodeScheme)
        }
    }
}

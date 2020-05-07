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
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.konan.KonanBundle
import com.jetbrains.mpp.execution.ApplePhysicalDevice
import com.jetbrains.mpp.execution.Device
import org.jdom.Element
import java.io.File
import java.util.regex.Pattern


class XCProjectFile(file: File) {
    val absolutePath: String = file.absolutePath

    val selector: String
        get() = if (absolutePath.endsWith(XCFileExtensions.workspace))
            "-workspace"
        else
            "-project"
}


class AppleRunConfiguration(project: Project, configurationFactory: MobileConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name), RunConfigurationWithSuppressedDefaultRunAction {

    private val xcodeproj: String?
        get() = ProjectWorkspace.getInstance(project).xcproject

    fun xcProjectFile(): XCProjectFile? {
        if (xcodeproj == null || project.basePath == null) {
            return null
        }

        val xcodeprojAbsoluteDirectory = FileUtil.join(project.basePath, xcodeproj)

        val projectFilePatterns = listOf(
            Pattern.compile(".+\\.${XCFileExtensions.workspace}"),
            Pattern.compile(".+\\.${XCFileExtensions.project}")
        )

        for (pattern in projectFilePatterns) {
            val files = FileUtil.findFilesOrDirsByMask(pattern, File(xcodeprojAbsoluteDirectory))

            files.minBy { it.absolutePath.lastIndexOf('/') }?.also {
                return XCProjectFile(it)
            }
        }

        return null
    }

    val xcodeScheme: String = "iosApp" // TODO: Use provided.

    fun xcodeSdk(target: ExecutionTarget): String {
        return if (target is ApplePhysicalDevice) "iphoneos" else "iphonesimulator"
    }

    val iosBuildDirectory = "ios_build" // TODO: Allow configuration.

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MobileRunConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        (environment.executionTarget as? Device)?.createState(this, environment)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
        val result = mutableListOf<BeforeRunTask<*>>()
        xcodeproj?.let { result.add(BuildIOSAppTask()) }
        return result
    }

    override fun checkConfiguration() {
        val propertyKey = KonanBundle.message("property.xcodeproj")
        if (xcodeproj == null) throw RuntimeConfigurationError("Can't find $propertyKey. Please, specify path relative to root to $propertyKey in your gradle.properties-file")
    }

    override fun canRunOn(target: ExecutionTarget): Boolean = target is Device

    fun getProductBundle(environment: ExecutionEnvironment): File {
        val buildType = if (environment.executionTarget is ApplePhysicalDevice) "Debug-iphoneos" else "Debug-iphonesimulator"
        if (project.basePath == null) throw RuntimeConfigurationError("Can't run ${this::class.simpleName} on project without base path.")
        return File(project.basePath).resolve(iosBuildDirectory).resolve("$buildType/$xcodeScheme.app")
    }

    var selectedDevice: Device? = null
}

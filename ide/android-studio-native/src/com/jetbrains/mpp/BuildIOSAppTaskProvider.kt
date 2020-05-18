/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import com.jetbrains.cidr.execution.ExecutionResult
import com.jetbrains.cidr.execution.build.CidrBuild
import com.jetbrains.cidr.execution.build.CidrBuild.startProcess
import com.jetbrains.cidr.execution.build.CidrBuildResult
import com.jetbrains.cidr.execution.build.CidrBuildTaskType
import com.jetbrains.kmm.XcProjectFile
import java.io.File


private val BUILD_IOS_APP_TASK_ID = Key.create<BuildIOSAppTask>(BuildIOSAppTask::class.java.name)

internal object XcFileExtensions {
    const val project = "xcodeproj"
    const val workspace = "xcworkspace"
}

class BuildIOSAppTask : BeforeRunTask<BuildIOSAppTask>(BUILD_IOS_APP_TASK_ID) {
    init {
        isEnabled = true
    }
}

private class BuildProcessListener(private val context: CidrBuild.BuildContext) : ProcessListener {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val scriptCommand = event.text.removeSuffix("\n")
        if (scriptCommand.isNotEmpty()) {
            context.indicator.text = scriptCommand
        }
    }

    override fun processTerminated(event: ProcessEvent) {
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
    }

    override fun startNotified(event: ProcessEvent) {
    }
}

private class BuildConfiguration : CidrBuildConfiguration {
    override fun getName(): String = "Xcode Build Configuration"
}

class BuildIOSAppTaskProvider : BeforeRunTaskProvider<BuildIOSAppTask>() {
    override fun getName() = "Build iOS app"

    override fun getId() = BUILD_IOS_APP_TASK_ID

    override fun createTask(runConfiguration: RunConfiguration): BuildIOSAppTask? =
        if (runConfiguration is AppleRunConfiguration) BuildIOSAppTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: BuildIOSAppTask
    ): Boolean {
        if (configuration !is AppleRunConfiguration) return false
        val workDirectory = configuration.project.basePath ?: return false
        val xcProjectFile = configuration.workspace.xcProjectFile ?: return false
        val xcodeScheme = configuration.xcodeScheme ?: return false

        val buildContext = CidrBuild.BuildContext(
            configuration.project,
            BuildConfiguration(),
            CidrBuildTaskType.BUILD,
            name,
            "Preparing build"
        )

        buildContext.processHandler = createBuildProcess(
            workDirectory,
            xcProjectFile,
            xcodeScheme,
            FileUtil.join(workDirectory, configuration.iosBuildDirectory),
            configuration.xcodeSdk(environment.executionTarget)
        )

        buildContext.processHandler.addProcessListener(BuildProcessListener(buildContext))

        val future = ApplicationManager.getApplication().executeOnPooledThread<ExecutionResult<CidrBuildResult>> {
            CidrBuild.execute(configuration.project, buildContext) {
                startProcess(configuration.project, buildContext, emptyList())
            }
        }

        return future.get().get().succeeded
    }

    private fun createBuildProcess(
        workDirectory: String,
        xcProjectFile: XcProjectFile,
        scheme: String,
        buildDirectory: String,
        sdk: String
    ): ProcessHandler {
        val cmd = GeneralCommandLine()

        cmd.workDirectory = File(workDirectory)

        // It seems that Xcode simply adds /usr/local/bin out of nowhere, but xcodebuild does not
        cmd.withEnvironment("PATH", "/usr/local/bin:" + cmd.parentEnvironment["PATH"])
        cmd.exePath = "/usr/bin/xcodebuild"
        cmd.addParameters(xcProjectFile.selector, xcProjectFile.absolutePath)
        cmd.addParameters("-scheme", scheme)
        cmd.addParameters("OBJROOT=$buildDirectory")
        cmd.addParameters("SYMROOT=$buildDirectory")
        cmd.addParameters("-sdk", sdk)

        return OSProcessHandler(cmd)
    }
}

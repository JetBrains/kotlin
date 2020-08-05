/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.build.BuildContentManagerImpl
import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.MessageEventResult
import com.intellij.build.events.impl.*
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
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import com.jetbrains.cidr.execution.ExecutionResult
import com.jetbrains.cidr.execution.build.CidrBuild
import com.jetbrains.cidr.execution.build.CidrBuild.startProcess
import com.jetbrains.cidr.execution.build.CidrBuildId
import com.jetbrains.cidr.execution.build.CidrBuildResult
import com.jetbrains.cidr.execution.build.CidrBuildTaskType
import com.jetbrains.kmm.KMM_LOG
import java.io.File


private val BUILD_IOS_APP_TASK_ID = Key.create<BuildIOSAppTask>(BuildIOSAppTask::class.java.name)

class BuildIOSAppTask : BeforeRunTask<BuildIOSAppTask>(BUILD_IOS_APP_TASK_ID) {
    init {
        isEnabled = true
    }
}

class BuildIOSAppTaskProvider : BeforeRunTaskProvider<BuildIOSAppTask>() {
    override fun getId() = BUILD_IOS_APP_TASK_ID
    override fun getName() = "Build iOS application"
    override fun getIcon() = AppleRunConfigurationType.ICON

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
        val xcProjectFile = configuration.xcProjectFile ?: return false
        val xcodeScheme = configuration.xcodeScheme ?: return false
        val xcodeSdk = configuration.xcodeSdk

        KMM_LOG.debug("executeTask: preparing build")
        val buildContext = CidrBuild.BuildContext(
            configuration.project,
            CidrBuildConfiguration { "Xcode Build Configuration" },
            CidrBuildTaskType.BUILD,
            name,
            "Preparing build"
        )

        buildContext.processHandler = createBuildProcess(
            workDirectory,
            xcProjectFile,
            xcodeScheme,
            FileUtil.join(workDirectory, configuration.iosBuildDirectory),
            xcodeSdk
        )

        buildContext.processHandler.addProcessListener(
            BuildProcessListener(
                configuration.project,
                name,
                workDirectory,
                buildContext.id
            )
        )

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

        KMM_LOG.info("createBuildProcess commandLine=" + cmd.commandLineString)
        return OSProcessHandler(cmd)
    }

    private class BuildProcessListener(
        project: Project,
        private val title: String,
        private val workDirectory: String,
        private val id: CidrBuildId
    ) : ProcessListener {
        private val buildView = ServiceManager.getService(
            project,
            BuildViewManager::class.java
        )
        private val buildWindow = ToolWindowManager.getInstance(project).getToolWindow(BuildContentManagerImpl.Build)

        override fun startNotified(event: ProcessEvent) {
            buildWindow?.activate(null, false)
            val startEvent = StartBuildEventImpl(
                DefaultBuildDescriptor(id, title, workDirectory, System.currentTimeMillis()),
                event.text.orEmpty()
            )
            buildView.onEvent(id, startEvent)
        }

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            buildView.onEvent(id, OutputBuildEventImpl(id, event.text.orEmpty(), true))
        }

        override fun processTerminated(event: ProcessEvent) {
            val finishEvent = if (event.exitCode == 0) {
                FinishBuildEventImpl(
                    id,
                    null,
                    System.currentTimeMillis(),
                    "success",
                    SuccessResultImpl()
                )
            } else {
                FinishBuildEventImpl(
                    id,
                    null,
                    System.currentTimeMillis(),
                    "failed with code: ${event.exitCode}",
                    AndroidStudioFailureResult()
                )
            }
            buildView.onEvent(id, finishEvent)
        }

        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}

        //Android Studio shows success result if FailureResultImpl doesn't contains list of failures ¯\_(ツ)_/¯
        //hence there is work around
        private class AndroidStudioFailureResult : FailureResultImpl(), MessageEventResult {
            override fun getKind() = MessageEvent.Kind.ERROR
        }
    }
}

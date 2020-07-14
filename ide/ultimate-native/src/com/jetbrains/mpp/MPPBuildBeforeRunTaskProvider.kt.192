/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.jetbrains.konan.KonanBundle
import com.jetbrains.konan.runBuildTasks


private val BUILD_BEFORE_RUN_TASK_ID = Key.create<MPPBuildBeforeRunTask>(MPPBuildBeforeRunTask::class.java.name)


class MPPBuildBeforeRunTask : BeforeRunTask<MPPBuildBeforeRunTask>(BUILD_BEFORE_RUN_TASK_ID) {
    init {
        isEnabled = true
    }
}


class MPPBuildBeforeRunTaskProvider : BeforeRunTaskProvider<MPPBuildBeforeRunTask>() {
    override fun getName() = "Build"

    override fun getId() = BUILD_BEFORE_RUN_TASK_ID

    override fun createTask(runConfiguration: RunConfiguration): MPPBuildBeforeRunTask? =
        if (runConfiguration is MPPBinaryRunConfiguration) MPPBuildBeforeRunTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: MPPBuildBeforeRunTask
    ): Boolean {
        if (configuration !is MPPBinaryRunConfiguration) return false
        val projectPath = configuration.project.basePath ?: return false

        with(configuration.selectedTarget!!) {
            return runBuildTasks(
                configuration.project,
                KonanBundle.message("execution.buildConfiguration.name", name),
                listOf(gradleTask),
                projectPath,
                false
            )
        }
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key


private val BUILD_BEFORE_RUN_TASK_ID = Key.create<IdeaKonanBuildBeforeRunTask>(IdeaKonanBuildBeforeRunTask::class.java.name)


class IdeaKonanBuildBeforeRunTask : BeforeRunTask<IdeaKonanBuildBeforeRunTask>(BUILD_BEFORE_RUN_TASK_ID) {
    init {
        isEnabled = true
    }
}


class IdeaKonanBuildBeforeRunTaskProvider : BeforeRunTaskProvider<IdeaKonanBuildBeforeRunTask>() {
    override fun getName() = "Build"

    override fun getId() = BUILD_BEFORE_RUN_TASK_ID

    override fun createTask(runConfiguration: RunConfiguration): IdeaKonanBuildBeforeRunTask? =
        if (runConfiguration is IdeaKonanRunConfiguration) IdeaKonanBuildBeforeRunTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: IdeaKonanBuildBeforeRunTask
    ): Boolean {
        if (configuration !is IdeaKonanRunConfiguration) return false
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

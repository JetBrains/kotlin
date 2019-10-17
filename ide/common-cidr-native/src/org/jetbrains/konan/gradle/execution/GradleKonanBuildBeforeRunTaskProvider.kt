/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.jetbrains.cidr.CidrBundle

class GradleKonanBuildBeforeRunTaskProvider : BeforeRunTaskProvider<GradleKotlinNativeBuildBeforeRunTask>() {

    override fun getId() = GradleKotlinNativeBuildBeforeRunTask.ID

    override fun getName() = CidrBundle.message("build")

    override fun getDescription(task: GradleKotlinNativeBuildBeforeRunTask?) = CidrBundle.message("build")

    override fun isSingleton() = true

    override fun createTask(runConfiguration: RunConfiguration): GradleKotlinNativeBuildBeforeRunTask? =
        if (runConfiguration is GradleKonanAppRunConfiguration) GradleKotlinNativeBuildBeforeRunTask() else null

    override fun canExecuteTask(configuration: RunConfiguration, task: GradleKotlinNativeBuildBeforeRunTask) =
        configuration is GradleKonanAppRunConfiguration

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        env: ExecutionEnvironment,
        task: GradleKotlinNativeBuildBeforeRunTask
    ) = if (configuration is GradleKonanAppRunConfiguration)
        GradleKonanBuild.buildBeforeRun(configuration.project, env, configuration)
    else false
}

class GradleKotlinNativeBuildBeforeRunTask : BeforeRunTask<GradleKotlinNativeBuildBeforeRunTask>(ID) {
    init {
        isEnabled = true
    }

    companion object {
        internal val ID = Key.create<GradleKotlinNativeBuildBeforeRunTask>(GradleKotlinNativeBuildBeforeRunTask::class.java.name)
    }
}

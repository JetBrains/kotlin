package com.jetbrains.konan

import com.intellij.build.BuildViewManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.concurrency.Semaphore
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.parseCompilerVersion
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

fun runBuildTasks(
    project: Project,
    executionName: String,
    taskNames: List<String>,
    projectPath: String,
    activateToolWindowBeforeRun: Boolean,
    env: Map<String, String>? = null
): Boolean {
    val settings = ExternalSystemTaskExecutionSettings().apply {
        this.executionName = executionName
        externalProjectPath = projectPath
        this.taskNames = taskNames
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        env?.let { this.env = it }
    }

    val userData = UserDataHolderBase()
    userData.putUserData(ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY, BuildViewManager::class.java)

    val result = Ref.create(false)
    val finished = Semaphore(1)
    val taskCallback = object : TaskCallback {
        override fun onSuccess() {
            result.set(true)
            finished.up()
        }

        override fun onFailure() {
            result.set(false)
            finished.up()
        }
    }

    ExternalSystemUtil.runTask(
        settings,
        DefaultRunExecutor.EXECUTOR_ID,
        project,
        GradleConstants.SYSTEM_ID,
        taskCallback,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
        activateToolWindowBeforeRun,
        userData
    )
    finished.waitFor()
    return result.get()
}

// Returns Kotlin/Native internal version (not the same as Big Kotlin version).
@Suppress("unused") // actually used in org.jetbrains.konan.debugger.KonanValueRendererFactoryKt.getPrettyPrintersLocation
fun getKotlinNativeVersion(kotlinNativeHome: String): CompilerVersion? {
    return try {
        Distribution(kotlinNativeHome).compilerVersion?.parseCompilerVersion()
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun filterOutSystemEnvs(user: Map<String, String>): MutableMap<String, String> {
    val result = LinkedHashMap<String, String>()
    val parental = GeneralCommandLine().parentEnvironment

    for ((key, value) in user) {
        if (!parental.containsKey(key)) {
            result[key] = value
        }
    }

    return result
}

package com.jetbrains.mobile.execution

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.task.*
import com.jetbrains.cidr.execution.build.runners.CidrProjectTaskRunner
import com.jetbrains.cidr.execution.build.runners.CidrTaskRunner
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class MobileProjectTaskRunner : CidrProjectTaskRunner() {
    override val buildSystemId: String
        get() = "Mobile IDE"

    @Suppress("UnstableApiUsage")
    class BuildableElement : ProjectModelBuildableElement {
        override fun getExternalSource(): ProjectModelExternalSource =
            object : ProjectModelExternalSource {
                override fun getId(): String = "MobileGradle"
                override fun getDisplayName(): String = "Mobile Gradle"
            }
    }

    override fun canRun(task: ProjectTask): Boolean = when (task) {
        is ProjectModelBuildTask<*> -> task.buildableElement is BuildableElement
        else -> false
    }

    override fun runnerForTask(task: ProjectTask, project: Project): CidrTaskRunner? = when (task) {
        is ProjectModelBuildTask<*> -> MobileBuildTaskRunner
        else -> null
    }
}

object MobileBuildTaskRunner : CidrTaskRunner {
    override fun executeTask(
        project: Project,
        task: ProjectTask,
        sessionId: Any,
        context: ProjectTaskContext
    ): Promise<ProjectTaskRunner.Result> {
        val success = try {
            val configuration = context.runConfiguration as MobileRunConfigurationBase
            MobileBuild.build(configuration, configuration.executionTargets)
        } catch (e: Throwable) {
            log.error(e)
            false
        }
        return resolvedPromise(if (success) TaskRunnerResults.FAILURE else TaskRunnerResults.SUCCESS)
    }

    private val log = logger<MobileBuildTaskRunner>()
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.task.*
import com.jetbrains.cidr.execution.build.CidrBuildUtil
import com.jetbrains.cidr.execution.build.runners.CidrProjectTaskRunner
import com.jetbrains.cidr.execution.build.runners.CidrTaskRunner
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class MobileProjectTaskRunner : CidrProjectTaskRunner() {
    @Suppress("UnstableApiUsage")
    class BuildableElement : ProjectModelBuildableElement {
        override fun getExternalSource(): ProjectModelExternalSource =
            object : ProjectModelExternalSource {
                override fun getId(): String = "MobileGradle"
                override fun getDisplayName(): String = "Mobile Gradle"
            }
    }

    class Context(sessionId: Any, runConfiguration: MobileRunConfiguration, val device: Device) :
        ProjectTaskContext(sessionId, runConfiguration)

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
            val configuration = (context as MobileProjectTaskRunner.Context).runConfiguration as MobileRunConfiguration
            MobileBuild.build(configuration, context.device)
        } catch (e: Throwable) {
            log.error(e)
            false
        }
        return resolvedPromise(if (success) TaskRunnerResults.FAILURE else TaskRunnerResults.SUCCESS)
    }

    private val log = logger<MobileBuildTaskRunner>()
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskProvider

/* This file is solely needed to suppress implicit dependencies on npm and yarn tasks registered by Kotlin Gradle Plugin */

private val rootNpmRelatedTasks = setOf("kotlinNpmInstall", "kotlinStoreYarnLock")
private val ownNpmRelatedTasks = setOf("jsGenerateExternalsIntegrated", "irGenerateExternalsIntegrated", "wasmGenerateExternalsIntegrated")

private val forbidImplicitDependOnNpmRelatedTasks = setOf(
    "jsPackageJson",
    "jsPublicPackageJson",
    "irPublicPackageJson",
    "legacyPublicPackageJson",
    "wasmPackageJson",
    "wasmPublicPackageJson",
    "compileKotlinJs",
    "compileKotlinJsIr",
    "compileKotlinWasm",
    "compileKotlinJsLegacy"
)
private val allowImplicitDependOnNpmForTasks = setOf("compileTestKotlinJs", "compileTestKotlinWasm")

/**
 * Kotlin JS gradle plugin implicitly define dependency to installing NPM and Yarn
 * https://youtrack.jetbrains.com/issue/KT-53687/Dont-trigger-npm-and-yarn-related-tasks-if-it-not-relevant-for-assemble
 * We would like to explicitly disable this behaviour for kotlin standard library publication as it's shouldn't be needed but make the
 * build slower, make it less stable, generated additional traffic, make the build less secure and so on.
 *
 * Implemented by manually removing dependencies set by Kotlin Gradle Plugin.
 */
fun Project.suppressYarnAndNpmForAssemble() {
    afterEvaluate {
        for (npmYarnTaskName in rootNpmRelatedTasks) {
            tasks.findByPath(":$npmYarnTaskName") ?: error("Can't find root task $npmYarnTaskName")
        }

        val npmRelatedTasksNames = rootNpmRelatedTasks + ownNpmRelatedTasks

        for (taskName in forbidImplicitDependOnNpmRelatedTasks) {
            val task = project.tasks.findByName(taskName) ?: continue
            if (!task.enabled) continue

            val removeDependencies = task.dependsOn
                .filterIsInstance(TaskProvider::class.java)
                .filter { it.name in npmRelatedTasksNames }
                .toSet()

            if (removeDependencies.isNotEmpty()) {
                task.setDependsOn(task.dependsOn - removeDependencies)
                logger.info("Disable NPM/Yarn dependency tasks in $project - " +
                                    "remove ${removeDependencies.joinToString(", ") { "'${it.name}'" }} dependencies from $task")
            }
        }
    }
}

private fun findRootTasks(taskGraph: TaskExecutionGraph): List<Task> {
    val allDependentTasksPaths = mutableSetOf<String>()
    val allTasksPaths = mutableSetOf<String>()
    taskGraph.allTasks.forEach { task ->
        allTasksPaths.add(task.path)
        for (dependency in task.taskDependencies.getDependencies(task)) {
            allDependentTasksPaths.add(dependency.path)
        }
    }
    val rootTasksPaths = allTasksPaths - allDependentTasksPaths

    return taskGraph.allTasks.filter { task -> task.path in rootTasksPaths }
}

private fun tasksGraphString(taskGraph: TaskExecutionGraph, ident: String = "", nextIdent: (String) -> String = { "$it|-"}): String {
    return buildString {
        val alreadyPrintedTasks = mutableSetOf<String>()
        fun Task.printTree(indent: String) {
            append(indent)
            if (path in alreadyPrintedTasks) {
                appendLine("$path *")
            } else {
                alreadyPrintedTasks.add(path)
                appendLine(path)
                val nextIndent = nextIdent(indent)
                for (dependency in taskDependencies.getDependencies(this)) {
                    dependency.printTree(nextIndent)
                }
            }
        }

        for (rootTask in findRootTasks(taskGraph)) {
            rootTask.printTree(ident)
        }
    }
}

val Project.checkYarnAndNPMSuppressed: Action<TaskExecutionGraph> get() {
    return Action<TaskExecutionGraph> {
        val disableNpmYarnCheck = providers.gradleProperty("kotlin.build.disable.npmyarn.suppress.check")
            .forUseAtConfigurationTime().orNull?.toBoolean() ?: false

        if (disableNpmYarnCheck) return@Action

        val executeTaskNames = allTasks.filter { it.enabled }.map { it.name }.toSet()

        val npmYarnTasks = rootNpmRelatedTasks.filter { it in executeTaskNames }
        val allowedTask = allowImplicitDependOnNpmForTasks.filter { it in executeTaskNames }

        if (npmYarnTasks.isNotEmpty()) {
            if (allowedTask.isEmpty()) {
                error("$npmYarnTasks tasks shouldn't be present in the task graph: $npmYarnTasks " +
                              "as $allowImplicitDependOnNpmForTasks tasks were not activated\n" +
                              "Graph:\n${tasksGraphString(this)}")
            }
        }
    }
}

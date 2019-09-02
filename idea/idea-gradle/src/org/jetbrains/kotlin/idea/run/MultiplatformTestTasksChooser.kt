/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ExternalSystemTestTask
import org.jetbrains.kotlin.idea.facet.externalSystemTestTasks
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.plugins.gradle.execution.test.runner.*
import org.jetbrains.plugins.gradle.util.TasksToRun
import java.util.*
import java.util.function.Consumer

class MultiplatformTestTasksChooser : TestTasksChooser() {
    fun multiplatformChooseTasks(
        project: Project,
        dataContext: DataContext,
        elements: Iterable<PsiElement>,
        handler: (List<Map<SourcePath, TestTasks>>) -> Unit
    ) {
        val consumer = Consumer<List<Map<SourcePath, TestTasks>>> { handler(it) }
        val testTasks = resolveTestTasks(elements)

        when {
            testTasks.isEmpty() -> super.chooseTestTasks(project, dataContext, elements, consumer)
            testTasks.size == 1 -> consumer.accept(testTasks.values.toList())
            else -> chooseTestTasks(project, dataContext, testTasks, consumer)
        }
    }

    private fun resolveTestTasks(elements: Iterable<PsiElement>): Map<TestName, Map<SourcePath, TasksToRun>> {
        val tasks = mutableMapOf<TestName, MutableMap<SourcePath, TasksToRun>>()

        for (element in elements) {
            val module = element.module ?: continue
            val sourceFile = getSourceFile(element) ?: continue

            val groupedTasks = module.externalSystemTestTasks().groupBy { it.targetName }

            for ((group, tasksInGroup) in groupedTasks) {
                if (tasksInGroup.isEmpty()) {
                    continue
                } else if (tasksInGroup.size == 1) {
                    val task = tasksInGroup[0]
                    val presentableName = task.presentableName
                    val tasksMap = tasks.getOrPut(presentableName) { LinkedHashMap() }
                    tasksMap[sourceFile.path] = TasksToRun.Impl(presentableName, getTaskNames(task))
                } else {
                    for (task in tasksInGroup) {
                        val rawTaskName = ':' + task.testName
                        val presentableName = if (group != null) "$group ($rawTaskName)" else rawTaskName
                        val tasksMap = tasks.getOrPut(presentableName) { LinkedHashMap() }
                        tasksMap[sourceFile.path] = TasksToRun.Impl(presentableName, getTaskNames(task))
                    }
                }
            }
        }

        return tasks
    }

    override fun chooseTestTasks(
        project: Project,
        context: DataContext,
        testTasks: Map<TestName, Map<SourcePath, TasksToRun>>,
        consumer: Consumer<List<Map<SourcePath, TestTasks>>>
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            val result = mutableListOf<Map<SourcePath, TestTasks>>()

            for (tasks in testTasks.values) {
                result += tasks.mapValues { it.value }
            }

            consumer.accept(result)
            return
        }

        super.chooseTestTasks(project, context, testTasks, consumer)
    }

    private fun getTaskNames(task: ExternalSystemTestTask): List<String> {
        return listOf("clean" + task.testName.capitalize(), task.testName)
    }
}

private val ExternalSystemTestTask.presentableName: String
    get() = targetName ?: (":$testName")
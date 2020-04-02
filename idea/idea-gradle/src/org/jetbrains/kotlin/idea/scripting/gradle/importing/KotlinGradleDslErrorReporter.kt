/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.build.FilePosition
import com.intellij.build.SyncViewManager
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemEventDispatcher
import com.intellij.openapi.project.Project
import java.io.File

private const val gradle_build_script_errors_group = "Kotlin Build Script Errors"

@Suppress("UnstableApiUsage")
class KotlinGradleDslErrorReporter(
    project: Project,
    private val task: ExternalSystemTaskId
) {

    private val syncViewManager = project.service<SyncViewManager>()
    private val buildEventDispatcher = ExternalSystemEventDispatcher(task, syncViewManager)

    fun reportError(
        scriptFile: File,
        model: KotlinDslScriptModel
    ) {
        model.messages.forEach {
            val severity = when (it.severity) {
                KotlinDslScriptModel.Severity.WARNING -> MessageEvent.Kind.WARNING
                KotlinDslScriptModel.Severity.ERROR -> MessageEvent.Kind.ERROR
            }
            val position = it.position
            if (position == null) {
                buildEventDispatcher.onEvent(
                    task,
                    MessageEventImpl(
                        task,
                        severity,
                        gradle_build_script_errors_group,
                        it.text,
                        it.details
                    )
                )
            } else {
                buildEventDispatcher.onEvent(
                    task,
                    FileMessageEventImpl(
                        task,
                        severity,
                        gradle_build_script_errors_group,
                        it.text, it.details,
                        // 0-based line numbers
                        FilePosition(scriptFile, position.line - 1, position.column)
                    ),
                )
            }
        }
    }
}

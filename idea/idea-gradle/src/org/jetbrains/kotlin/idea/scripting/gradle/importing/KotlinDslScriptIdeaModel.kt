/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.util.NotNullableCopyableDataNodeUserDataProperty

var DataNode<out ProjectData>.KOTLIN_DSL_SCRIPT_IDEA_MODELS: MutableList<KotlinDslScriptIdeaModel>
        by NotNullableCopyableDataNodeUserDataProperty(
            Key.create<MutableList<KotlinDslScriptIdeaModel>>(
                "GRADLE_KOTLIN_BUILD_SCRIPTS"
            ),
            mutableListOf()
        )

data class KotlinDslScriptIdeaModel(
    val file: String,
    val classPath: List<String>,
    val sourcePath: List<String>,
    val imports: List<String>,
    val messages: List<Message>
) {
    data class Message(
        val severity: Severity,
        val text: String,
        val position: Position? = null
    )

    data class Position(val line: Int, val column: Int)

    enum class Severity { WARNING, ERROR }
}

@Suppress("unused")
fun KotlinDslScriptModelResolver.copy(model: KotlinDslScriptsModel): List<KotlinDslScriptIdeaModel> {
    return model.scriptModels.map { (file, model) ->
        val messages = mutableListOf<KotlinDslScriptIdeaModel.Message>()

        model.exceptions.forEach {
            messages.add(
                KotlinDslScriptIdeaModel.Message(
                    KotlinDslScriptIdeaModel.Severity.ERROR, it
                )
            )
        }

        model.editorReports.forEach {
            messages.add(KotlinDslScriptIdeaModel.Message(
                when (it.severity) {
                    EditorReportSeverity.WARNING -> KotlinDslScriptIdeaModel.Severity.WARNING
                    else -> KotlinDslScriptIdeaModel.Severity.ERROR
                },
                it.message,
                it.position?.let { position ->
                    KotlinDslScriptIdeaModel
                        .Position(position.line, position.column)
                }
            ))
        }

        KotlinDslScriptIdeaModel(
            file.absolutePath,
            model.classPath.map { it.absolutePath },
            model.sourcePath.map { it.absolutePath },
            model.implicitImports,
            messages
        )
    }
}
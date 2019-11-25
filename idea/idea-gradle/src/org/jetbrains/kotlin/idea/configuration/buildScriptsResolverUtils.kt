/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.util.NotNullableCopyableDataNodeUserDataProperty


var DataNode<out ProjectData>.gradleKotlinBuildScripts
        by NotNullableCopyableDataNodeUserDataProperty(
            Key.create<List<GradleKotlinBuildScriptData>>(
                "GRADLE_KOTLIN_BUILD_SCRIPTS"
            ),
            emptyList()
        )

data class GradleKotlinBuildScriptData(
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
fun KotlinGradleBuildScriptsResolver.copy(model: KotlinDslScriptsModel): List<GradleKotlinBuildScriptData> {
    return model.scriptModels.map { (file, model) ->
        val messages = mutableListOf<GradleKotlinBuildScriptData.Message>()

        model.exceptions.forEach {
            messages.add(
                GradleKotlinBuildScriptData.Message(GradleKotlinBuildScriptData.Severity.ERROR, it)
            )
        }

        model.editorReports.forEach {
            messages.add(GradleKotlinBuildScriptData.Message(
                when (it.severity) {
                    EditorReportSeverity.WARNING -> GradleKotlinBuildScriptData.Severity.WARNING
                    else -> GradleKotlinBuildScriptData.Severity.ERROR
                },
                it.message,
                it.position?.let { position ->
                    GradleKotlinBuildScriptData.Position(position.line, position.column)
                }
            ))
        }

        GradleKotlinBuildScriptData(
            file.absolutePath,
            model.classPath.map { it.absolutePath },
            model.sourcePath.map { it.absolutePath },
            model.implicitImports,
            messages
        )
    }
}
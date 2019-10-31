/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.util.Consumer
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters.*
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.util.NotNullableCopyableDataNodeUserDataProperty
import org.jetbrains.plugins.gradle.model.ProjectImportExtraModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

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

var DataNode<out ProjectData>.gradleKotlinBuildScripts
        by NotNullableCopyableDataNodeUserDataProperty(
            Key.create<List<GradleKotlinBuildScriptData>>("GRADLE_KOTLIN_BUILD_SCRIPTS"),
            emptyList()
        )

class KotlinGradleBuildScriptsResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinDslScriptsModel::class.java)
    }

    override fun getExtraJvmArgs(): List<Pair<String, String>> {
        return listOf(
            Pair(
                "-D$PROVIDER_MODE_SYSTEM_PROPERTY_NAME",
                STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE
            )
        )
    }

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        initScriptConsumer.consume("startParameter.taskNames += [\"${PREPARATION_TASK_NAME}\"]")
    }

    override fun getExtraModelProvider(): ProjectImportExtraModelProvider {
        return KotlinDslScriptModelProvider()
    }

    override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
        super.populateProjectExtraModels(gradleProject, ideProject)

        resolverCtx.getExtraProject(null, KotlinDslScriptsModel::class.java)?.let { model ->
            // we need a copy to avoid memory leak, as model is java rmi proxy object
            ideProject.gradleKotlinBuildScripts = copy(model)
        }
    }

    override fun requiresTaskRunning() = true

    fun copy(model: KotlinDslScriptsModel): List<GradleKotlinBuildScriptData> {
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
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptInputsWatcher
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptingSupport
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.idea.scripting.gradle.getJavaHomeForGradleProject
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun processScriptModel(
    resolverCtx: ProjectResolverContext,
    model: KotlinDslScriptsModel,
    projectName: String
) {
    if (model is BrokenKotlinDslScriptsModel) {
        LOG.error(
            "Couldn't get KotlinDslScriptsModel for $projectName:\n${model.message}\n${model.stackTrace}"
        )
    } else {
        val task = resolverCtx.externalSystemTaskId
        val project = task.findProject() ?: return
        val models = model.toListOfScriptModels(project)

        val externalTaskId = task.toExternalTaskId()

        project.kotlinDslModels
            .getOrPut(externalTaskId) {
                KotlinDslScriptModelsForGradleProject()
            }.apply {
                this.gradleProjectPaths.add(resolverCtx.projectPath)
                this.models.addAll(models)
            }

        if (models.containsErrors()) {
            throw IllegalStateException(KotlinIdeaGradleBundle.message("title.kotlin.build.script"))
        }
    }
}

private fun Collection<KotlinDslScriptModel>.containsErrors(): Boolean {
    return any { it.messages.any { it.severity == KotlinDslScriptModel.Severity.ERROR } }
}

private fun KotlinDslScriptsModel.toListOfScriptModels(project: Project): List<KotlinDslScriptModel> =
    scriptModels.map { (file, model) ->
        val messages = mutableListOf<KotlinDslScriptModel.Message>()

        model.exceptions.forEach {
            val fromException = parsePositionFromException(it)
            if (fromException != null) {
                val (filePath, _) = fromException
                if (filePath != file.path) return@forEach
            }
            messages.add(
                KotlinDslScriptModel.Message(
                    KotlinDslScriptModel.Severity.ERROR,
                    it.substringBefore(System.lineSeparator()),
                    it,
                    fromException?.second
                )
            )
        }

        model.editorReports.forEach {
            messages.add(
                KotlinDslScriptModel.Message(
                    when (it.severity) {
                        EditorReportSeverity.WARNING -> KotlinDslScriptModel.Severity.WARNING
                        else -> KotlinDslScriptModel.Severity.ERROR
                    },
                    it.message,
                    position = KotlinDslScriptModel.Position(it.position?.line ?: 0, it.position?.column ?: 0)
                )
            )
        }

        // TODO: NPE
        val virtualFile = VfsUtil.findFile(file.toPath(), true)!!

        // todo(KT-34440): take inputs snapshot before starting import
        KotlinDslScriptModel(
            file.absolutePath,
            // TODO: NPE
            getGradleScriptInputsStamp(project, virtualFile)!!,
            model.classPath.map { it.absolutePath },
            model.sourcePath.map { it.absolutePath },
            model.implicitImports,
            messages
        )
    }

fun createGradleKtsContextIfPossible(project: Project): GradleKtsContext? {
    val javaHome = getJavaHomeForGradleProject(project)?.let { File(it) }

    return GradleKtsContext(javaHome)
}

class GradleKtsContext(val javaHome: File?)

fun KotlinDslScriptModel.toScriptConfiguration(context: GradleKtsContext, project: Project): ScriptCompilationConfigurationWrapper? {
    val scriptFile = File(file)
    val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

    val definition = virtualFile.findScriptDefinition(project) ?: return null

    return ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
        VirtualFileScriptSource(virtualFile),
        definition.compilationConfiguration.with {
            if (context.javaHome != null) {
                jvm.jdkHome(context.javaHome)
            }
            defaultImports(imports)
            dependencies(JvmDependency(classPath.map { File(it) }))
            ide.dependenciesSources(JvmDependency(sourcePath.map { File(it) }))
        }.adjustByDefinition(definition)
    )
}

fun saveScriptModels(
    project: Project,
    task: ExternalSystemTaskId,
    javaHomeStr: String?,
    modelsForGradleProject: KotlinDslScriptModelsForGradleProject
) {
    val errorReporter = KotlinGradleDslErrorReporter(project, task)

    val javaHome = javaHomeStr?.let { File(it) }
    val context = GradleKtsContext(javaHome)

    modelsForGradleProject.models.forEach { model ->
        errorReporter.reportError(File(model.file), model)
    }

    project.service<GradleScriptInputsWatcher>().saveGradleProjectRootsAfterImport(
        modelsForGradleProject.models.map { FileUtil.toSystemIndependentName(File(it.file).parent) }.toSet()
    )

    GradleScriptingSupport.getInstance(project).replace(
        context, modelsForGradleProject.gradleProjectId to modelsForGradleProject.models.toList()
    )
    project.service<GradleScriptInputsWatcher>().clearState()
}

data class GradleProjectId(val paths: List<Int>)
data class ExternalTaskId(val id: String)


fun ExternalSystemTaskId.toExternalTaskId(): ExternalTaskId {
    return ExternalTaskId(this.ideProjectId + ":" + hashCode())
}
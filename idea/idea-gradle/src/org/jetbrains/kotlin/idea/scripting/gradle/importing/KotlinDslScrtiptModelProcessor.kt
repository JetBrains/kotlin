/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptInputsWatcher
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
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
        val models = model.toListOfScriptModels()

        saveScriptModels(resolverCtx, models)

        if (models.containsErrors()) {
            throw IllegalStateException(KotlinIdeaGradleBundle.message("title.kotlin.build.script"))
        }
    }
}

private fun Collection<KotlinDslScriptModel>.containsErrors(): Boolean {
    return any { it.messages.any { it.severity == KotlinDslScriptModel.Severity.ERROR } }
}

private fun KotlinDslScriptsModel.toListOfScriptModels(): List<KotlinDslScriptModel> =
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

        // todo(KT-34440): take inputs snapshot before starting import
        KotlinDslScriptModel(
            file.absolutePath,
            System.currentTimeMillis(),
            model.classPath.map { it.absolutePath },
            model.sourcePath.map { it.absolutePath },
            model.implicitImports,
            messages
        )
    }

fun saveScriptModels(
    resolverContext: ProjectResolverContext,
    models: List<KotlinDslScriptModel>
) {
    val task = resolverContext.externalSystemTaskId
    val project = task.findProject() ?: return
    val settings = resolverContext.settings ?: return

    val scriptConfigurations = mutableListOf<Pair<VirtualFile, ScriptConfigurationSnapshot>>()

    val errorReporter = KotlinGradleDslErrorReporter(project, task)

    val javaHome = settings.javaHome?.let { File(it) }
    models.forEach { model ->
        val scriptFile = File(model.file)
        val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

        val inputs = getGradleScriptInputsStamp(
            project,
            virtualFile,
            givenTimeStamp = model.inputsTimeStamp
        )

        val definition = virtualFile.findScriptDefinition(project) ?: return@forEach

        val configuration =
            definition.compilationConfiguration.with {
                if (javaHome != null) {
                    jvm.jdkHome(javaHome)
                }
                defaultImports(model.imports)
                dependencies(JvmDependency(model.classPath.map {
                    File(
                        it
                    )
                }))
                ide.dependenciesSources(JvmDependency(model.sourcePath.map {
                    File(
                        it
                    )
                }))
            }.adjustByDefinition(definition)

        scriptConfigurations.add(
            Pair(
                virtualFile,
                ScriptConfigurationSnapshot(
                    inputs
                        ?: CachedConfigurationInputs.OutOfDate,
                    listOf(),
                    ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                        VirtualFileScriptSource(virtualFile),
                        configuration,
                    ),
                ),
            ),
        )

        errorReporter.reportError(scriptFile, model)
    }

    project.service<GradleScriptInputsWatcher>().saveGradleProjectRootsAfterImport(
        scriptConfigurations.map { it.first.parent.path }.toSet()
    )

    project.service<ScriptConfigurationManager>().saveCompilationConfigurationAfterImport(scriptConfigurations)
    project.service<GradleScriptInputsWatcher>().clearState()
}
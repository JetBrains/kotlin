/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemEventDispatcher
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptInputsWatcher
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.idea.scripting.gradle.saveGradleProjectRootsAfterImport
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun saveScriptModels(
    resolverContext: ProjectResolverContext,
    models: List<KotlinDslScriptModel>
) {
    val task = resolverContext.externalSystemTaskId
    val project = task.findProject() ?: return
    val settings = resolverContext.settings ?: return

    val scriptConfigurations = mutableListOf<Pair<VirtualFile, ScriptConfigurationSnapshot>>()

    val syncViewManager = project.service<SyncViewManager>()
    val buildEventDispatcher =
        ExternalSystemEventDispatcher(task, syncViewManager)

    val javaHome = settings.javaHome?.let { File(it) }
    models.forEach { buildScript ->
        val scriptFile = File(buildScript.file)
        val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

        val inputs = getGradleScriptInputsStamp(
            project,
            virtualFile,
            givenTimeStamp = buildScript.inputsTimeStamp
        )

        val definition = virtualFile.findScriptDefinition(project) ?: return@forEach

        val configuration =
            definition.compilationConfiguration.with {
                if (javaHome != null) {
                    jvm.jdkHome(javaHome)
                }
                defaultImports(buildScript.imports)
                dependencies(JvmDependency(buildScript.classPath.map {
                    File(
                        it
                    )
                }))
                ide.dependenciesSources(JvmDependency(buildScript.sourcePath.map {
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

        buildScript.messages.forEach {
            addBuildScriptDiagnosticMessage(it, virtualFile, project)
        }
    }

    saveGradleProjectRootsAfterImport(
        scriptConfigurations.map { it.first.parent.path }.toSet()
    )

    project.service<ScriptConfigurationManager>().saveCompilationConfigurationAfterImport(scriptConfigurations)
    project.service<GradleScriptInputsWatcher>().clearState()
}

private fun addBuildScriptDiagnosticMessage(
    message: KotlinDslScriptModel.Message,
    virtualFile: VirtualFile,
    project: Project
) {
    val notification = NotificationData(
        KotlinIdeaGradleBundle.message("title.kotlin.build.script"),
        message.text,
        when (message.severity) {
            KotlinDslScriptModel.Severity.WARNING -> NotificationCategory.WARNING
            KotlinDslScriptModel.Severity.ERROR -> NotificationCategory.ERROR
        },
        NotificationSource.PROJECT_SYNC
    )

    notification.navigatable =
        LazyNavigatable(
            virtualFile,
            project,
            message.position
        )

    ExternalSystemNotificationManager.getInstance(project).showNotification(
        GradleConstants.SYSTEM_ID,
        notification
    )
}

class LazyNavigatable internal constructor(
    private val virtualFile: VirtualFile,
    private val project: Project,
    val position: KotlinDslScriptModel.Position?
) : Navigatable {
    private val openFileDescriptor: Navigatable by lazy {
        if (position != null) OpenFileDescriptor(project, virtualFile, position.line, position.column)
        else OpenFileDescriptor(project, virtualFile, -1)
    }

    override fun navigate(requestFocus: Boolean) {
        if (openFileDescriptor.canNavigate()) openFileDescriptor.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = virtualFile.exists()

    override fun canNavigateToSource(): Boolean = canNavigate()
}
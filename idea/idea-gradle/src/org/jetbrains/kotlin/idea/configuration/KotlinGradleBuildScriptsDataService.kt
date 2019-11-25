/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

class KotlinGradleBuildScriptsDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey(): Key<GradleSourceSetData> = GradleSourceSetData.KEY

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<GradleSourceSetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        super.onSuccessImport(imported, projectData, project, modelsProvider)

        val projectDataNode = imported.firstNotNullResult { ExternalSystemApiUtil.findParent(it, ProjectKeys.PROJECT) } ?: return

        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val projectSettings = gradleSettings.getLinkedProjectSettings(projectData?.linkedExternalProjectPath ?: return) ?: return
        val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
            project,
            projectSettings.externalProjectPath,
            GradleConstants.SYSTEM_ID
        )
        val javaHome = File(gradleExeSettings.javaHome ?: return)

        val files = mutableListOf<Pair<VirtualFile, ScriptConfigurationSnapshot>>()

        projectDataNode.gradleKotlinBuildScripts?.forEach { buildScript ->
            val scriptFile = File(buildScript.file)
            val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

            // todo(KT-34440): take inputs snapshot before starting import
            val inputs = getGradleScriptInputsStamp(project, virtualFile)

            val definition = virtualFile.findScriptDefinition(project) ?: return@forEach

            val configuration =
                definition.compilationConfiguration.with {
                    jvm.jdkHome(javaHome)
                    defaultImports(buildScript.imports)
                    dependencies(JvmDependency(buildScript.classPath.map { File(it) }))
                    ide.dependenciesSources(JvmDependency(buildScript.sourcePath.map { File(it) }))
                }.adjustByDefinition(definition)

            files.add(
                Pair(
                    virtualFile,
                    ScriptConfigurationSnapshot(
                        inputs ?: CachedConfigurationInputs.OutOfDate,
                        listOf(),
                        ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                            VirtualFileScriptSource(virtualFile),
                            configuration
                        )
                    )
                )
            )

            buildScript.messages.forEach {
                addBuildScriptDiagnosticMessage(it, virtualFile, project)
            }
        }

        project.service<ScriptConfigurationManager>().saveCompilationConfigurationAfterImport(files)
    }

    private fun addBuildScriptDiagnosticMessage(
        message: GradleKotlinBuildScriptData.Message,
        virtualFile: VirtualFile,
        project: Project
    ) {
        val notification = NotificationData(
            "Kotlin Build Script",
            message.text,
            when (message.severity) {
                GradleKotlinBuildScriptData.Severity.WARNING -> NotificationCategory.WARNING
                GradleKotlinBuildScriptData.Severity.ERROR -> NotificationCategory.ERROR
            },
            NotificationSource.PROJECT_SYNC
        )

        notification.navigatable = LazyNavigatable(
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
        val position: GradleKotlinBuildScriptData.Position?
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
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

sealed class GradleBuildRoot(
    val dir: VirtualFile
) {
    abstract class Unlinked(root: VirtualFile) : GradleBuildRoot(root)

    abstract class Linked(
        root: VirtualFile,
        val settings: GradleProjectSettings
    ) : GradleBuildRoot(root) {
        val importing = false
    }

    class Legacy(
        root: VirtualFile,
        settings: GradleProjectSettings
    ) : Linked(root, settings)

    class New(
        root: VirtualFile,
        settings: GradleProjectSettings
    ) : Linked(root, settings)

    class Imported(
        val project: Project,
        root: VirtualFile,
        settings: GradleProjectSettings,
        val context: GradleKtsContext,
        val data: GradleImportedBuildRootData
    ) : Linked(root, settings) {
        val scriptDefinition: ScriptDefinition? by lazy {
            val anyScript = data.models.firstOrNull()?.let {
                LocalFileSystem.getInstance().findFileByPath(it.file)
            }

            anyScript?.findScriptDefinition(project)
        }

        fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
            val javaHome = context.javaHome
            javaHome?.let { builder.addSdk(it) }

            if (scriptDefinition != null) {
                builder.classes.addAll(data.templateClasspath)
            }

            data.models.forEach {
                if (scriptDefinition != null) {
                    builder.scripts[it.file] = ScriptInfo(this, it)
                }

                builder.classes.addAll(it.classPath)
                builder.sources.addAll(it.sourcePath)
            }
        }

        class ScriptInfo(
            val buildRoot: Imported,
            val model: KotlinDslScriptModel
        ) : ScriptClassRootsCache.LightScriptInfo() {
            override fun buildConfiguration(): ScriptCompilationConfigurationWrapper {
                val javaHome = buildRoot.context.javaHome
                val scriptDefinition = buildRoot.scriptDefinition!!

                val scriptFile = File(model.file)
                val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

                return ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                    VirtualFileScriptSource(virtualFile),
                    scriptDefinition.compilationConfiguration.with {
                        if (javaHome != null) {
                            jvm.jdkHome(javaHome)
                        }
                        defaultImports(model.imports)
                        dependencies(JvmDependency(model.classPath.map { File(it) }))
                        ide.dependenciesSources(JvmDependency(model.sourcePath.map { File(it) }))
                    }.adjustByDefinition(scriptDefinition)
                )
            }
        }
    }
}
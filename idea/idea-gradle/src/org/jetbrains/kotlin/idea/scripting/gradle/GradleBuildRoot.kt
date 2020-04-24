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
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

sealed class GradleBuildRoot {
    class Unlinked : GradleBuildRoot()

    abstract class Linked : GradleBuildRoot() {
        val importing = false
    }

    class Legacy : Linked()

    class New : Linked()

    class Imported(
        val project: Project,
        val dir: VirtualFile,
        val context: GradleKtsContext,
        val data: GradleImportedBuildRootData
    ) : Linked() {
        fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
            val javaHome = context.javaHome
            javaHome?.let { builder.addSdk(it) }

            val anyScript = data.models.firstOrNull()?.let {
                LocalFileSystem.getInstance().findFileByPath(it.file)
            }

            val scriptDefinition: ScriptDefinition? = anyScript?.findScriptDefinition(project)

            if (scriptDefinition != null) {
                builder.classes.addAll(data.templateClasspath)
            }

            data.models.forEach {
                builder.scripts[it.file] = ScriptInfo(this, scriptDefinition, it)

                builder.classes.addAll(it.classPath)
                builder.sources.addAll(it.sourcePath)
            }
        }

        class ScriptInfo(
            val buildRoot: Imported,
            val scriptDefinition: ScriptDefinition?,
            val model: KotlinDslScriptModel
        ) : ScriptClassRootsCache.LightScriptInfo() {
            override fun buildConfiguration(): ScriptCompilationConfigurationWrapper? {
                val javaHome = buildRoot.context.javaHome
                val scriptDefinition = scriptDefinition

                val scriptFile = File(model.file)
                val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

                if (scriptDefinition == null) return null

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
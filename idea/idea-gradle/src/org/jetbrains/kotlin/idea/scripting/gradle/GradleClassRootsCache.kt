/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache.Companion.getScriptSdkOrDefault
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage.Companion.ScriptClassRoots
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

class GradleClassRootsCache(
    val project: Project,
    val context: GradleKtsContext,
    val configuration: Configuration
) : ScriptClassRootsCache(
    project,
    ScriptClassRootsStorage.Companion.Key("gradle"),
    extractRoots(context, configuration, project)
) {
    override fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        configuration.scriptModel(file)?.toScriptConfiguration()

    private fun KotlinDslScriptModel.toScriptConfiguration(): ScriptCompilationConfigurationWrapper? {
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

    override fun getScriptSdk(file: VirtualFile): Sdk? {
        return firstScriptSdk
    }

    override val firstScriptSdk: Sdk? = getScriptSdkOrDefault(context.javaHome, project)

    // called to ensure that configuration for file is loaded
    // as we cannot force loading, we always return true
    override fun contains(file: VirtualFile): Boolean = true

    companion object {
        fun extractRoots(context: GradleKtsContext, configuration: Configuration?, project: Project): ScriptClassRoots {
            if (configuration == null) {
                return ScriptClassRootsStorage.EMPTY
            }
            val scriptSdk = getScriptSdkOrDefault(context.javaHome, project)
            if (scriptSdk != null && !scriptSdk.isAlreadyIndexed(project)) {
                return ScriptClassRootsStorage.Companion.ScriptClassRoots(
                    configuration.classFilePath,
                    configuration.sourcePath,
                    setOf(scriptSdk)
                )
            }
            return ScriptClassRoots(
                configuration.classFilePath,
                configuration.sourcePath,
                getScriptSdkOrDefault(context.javaHome, project)?.let { setOf(it) } ?: setOf()
            )
        }
    }
}
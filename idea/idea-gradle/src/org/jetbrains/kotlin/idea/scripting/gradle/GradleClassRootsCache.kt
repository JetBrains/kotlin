/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage.Companion.ScriptClassRoots
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import java.io.File
import java.nio.file.FileSystems
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun GradleScriptingSupport.createRootsCache(): ScriptClassRootsCache? {
    if (configuration.data.models.isEmpty()) return null

    val key = ScriptClassRootsStorage.Companion.Key(buildRoot.path)
    val sdk = ScriptClassRootsCache.getScriptSdk(context.javaHome) ?: return null

    val anyScriptPathString = configuration.data.models.first().file
    val anyScriptPath = FileSystems.getDefault().getPath(anyScriptPathString)
    val anyScriptVFile = VfsUtil.findFile(anyScriptPath, true) ?: return null

    val definition = anyScriptVFile.findScriptDefinition(project) ?: return null

    val scriptRoots = ScriptClassRoots(
        configuration.classFilePath,
        configuration.sourcePath,
        setOf(sdk)
    )

    fun KotlinDslScriptModel.toScriptConfiguration(): ScriptCompilationConfigurationWrapper? {
        val scriptFile = File(file)
        val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

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

    return object : ScriptClassRootsCache(project, key, scriptRoots) {
        override fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
            configuration.scriptModel(file)?.toScriptConfiguration()

        override fun getScriptSdk(file: VirtualFile) = sdk

        override val firstScriptSdk: Sdk? = sdk

        // called to ensure that configuration for file is loaded
        // as we cannot force loading, we always return true
        override fun contains(file: VirtualFile): Boolean = true
    }
}
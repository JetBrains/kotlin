/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

class GradleScriptInfo(
    val buildRoot: Imported,
    scriptDefinition: ScriptDefinition?,
    val model: KotlinDslScriptModel
) : ScriptClassRootsCache.LightScriptInfo(scriptDefinition) {
    override fun buildConfiguration(): ScriptCompilationConfigurationWrapper? {
        val javaHome = buildRoot.javaHome

        val scriptFile = File(model.file)
        val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

        if (definition == null) return null

        return ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
            VirtualFileScriptSource(virtualFile),
            definition.compilationConfiguration.with {
                if (javaHome != null) {
                    jvm.jdkHome(javaHome)
                }
                defaultImports(model.imports)
                dependencies(JvmDependency(model.classPath.map { File(it) }))
                ide.dependenciesSources(JvmDependency(model.sourcePath.map { File(it) }))
            }.adjustByDefinition(definition)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradleScriptInfo

        if (buildRoot.pathPrefix != other.buildRoot.pathPrefix) return false
        if (model != other.model) return false
        if (definition != other.definition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = buildRoot.pathPrefix.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + definition.hashCode()
        return result
    }
}

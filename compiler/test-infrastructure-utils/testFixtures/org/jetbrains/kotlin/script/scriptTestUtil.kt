/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

fun loadScriptingPlugin(configuration: CompilerConfiguration, parentDisposable: Disposable) {
    val pluginClasspath = extractFromPropertyFirstFiles(KOTLIN_SCRIPTING_PLUGIN_CLASSPATH) {
        val libPath = PathUtil.kotlinPathsForCompiler.libPath
        with(PathUtil) {
            listOf(
                KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR,
                KOTLIN_SCRIPTING_COMPILER_IMPL_JAR,
                KOTLIN_SCRIPTING_COMMON_JAR,
                KOTLIN_SCRIPTING_JVM_JAR
            ).map { File(libPath, it) }
        }
    }

    PluginCliParser.loadPluginsSafe(pluginClasspath.map { it.path }, emptyList(), emptyList(), emptyList(), configuration, parentDisposable)
}

fun loadScriptingPlugin(configuration: CompilerConfiguration, parentDisposable: Disposable, pluginClasspath: Collection<String>) {
    PluginCliParser.loadPluginsSafe(pluginClasspath, emptyList(), emptyList(), emptyList(), configuration, parentDisposable)
}

private inline fun extractFromPropertyFirstFiles(property: String, onMissingProperty: () -> Collection<File>): Collection<File> {
    return System.getProperty(property)?.split(",")?.map {
        File(it).also { file ->
            assert(file.exists()) { "$it not found" }
        }
    } ?: onMissingProperty()
}

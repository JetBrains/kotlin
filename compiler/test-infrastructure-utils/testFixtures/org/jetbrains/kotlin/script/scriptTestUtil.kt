/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration

fun loadScriptingPlugin(configuration: CompilerConfiguration, parentDisposable: Disposable) {
    val pluginClasspath = ForTestCompileRuntime.scriptingPluginFilesForTests()
    PluginCliParser.loadPluginsSafe(pluginClasspath.map { it.path }, emptyList(), emptyList(), emptyList(), configuration, parentDisposable)
}

fun loadScriptingPlugin(configuration: CompilerConfiguration, parentDisposable: Disposable, pluginClasspath: Collection<String>) {
    PluginCliParser.loadPluginsSafe(pluginClasspath, emptyList(), emptyList(), emptyList(), configuration, parentDisposable)
}


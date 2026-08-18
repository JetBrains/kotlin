/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.plugins

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.Services

/**
 * Represents a loader for plugins that can dynamically load Kotlin compiler plugins and their
 * associated command-line processors from a specified classpath.
 *
 * When provided through compiler [Services] it allows for overriding the default plugin loading behavior to provide custom logic,
 * such as but not limited to, customizing the classloading mechanism, adding caching, or injecting different versions of plugins.
 */
interface PluginsLoader {
    fun loadCompilerPluginRegistrars(pluginClasspath: Collection<String>, parentDisposable: Disposable): List<CompilerPluginRegistrar>
    fun loadCommandLineProcessors(pluginClasspath: Collection<String>, parentDisposable: Disposable): List<CommandLineProcessor>
}

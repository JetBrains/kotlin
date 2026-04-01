/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor

@ExperimentalCompilerApi
abstract class CompilerPluginRegistrar {
    companion object {
        val COMPILER_PLUGIN_REGISTRARS: CompilerConfigurationKey<MutableList<CompilerPluginRegistrar>> =
            CompilerConfigurationKey.create("COMPILER_PLUGIN_REGISTRARS")
    }

    /**
     * Uniquely identifies the Kotlin compiler plugin. Must match the `pluginId` specified in [CommandLineProcessor].
     * The ID can be used in combination with `-Xcompiler-plugin-order` to control execution order of compiler plugins.
     */
    abstract val pluginId: String

    abstract fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration)

    class ExtensionStorage {
        val registeredExtensions: Map<ExtensionPointDescriptor<*>, List<Any>>
            field = mutableMapOf<ExtensionPointDescriptor<*>, MutableList<Any>>()

        val disposables: List<PluginDisposable>
            field = mutableListOf<PluginDisposable>()

        operator fun <T : Any> get(descriptor: ExtensionPointDescriptor<T>): List<T> {
            @Suppress("UNCHECKED_CAST")
            return registeredExtensions[descriptor] as List<T>? ?: emptyList()
        }

        fun <T : Any> ExtensionPointDescriptor<T>.registerExtension(extension: T) {
            registeredExtensions.getOrPut(this, ::mutableListOf).add(extension)
        }

        /**
         * Passed [disposable] will be called when the plugin is no longer needed:
         *
         * - In the CLI mode: At the end of the compilation process.
         * - In the IDE mode: When the whole project is closed, or when the module
         * with the corresponding compiler plugin enabled is removed from the project.
         */
        @Suppress("unused")
        fun registerDisposable(disposable: PluginDisposable) {
            disposables += disposable
        }
    }

    fun interface PluginDisposable {
        fun dispose()
    }

    abstract val supportsK2: Boolean
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

@ExperimentalCompilerApi
abstract class CompilerPluginRegistrar {
    companion object {
        val COMPILER_PLUGIN_REGISTRARS: CompilerConfigurationKey<MutableList<CompilerPluginRegistrar>> =
            CompilerConfigurationKey.create("Compiler plugin registrars")
    }

    abstract fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration)

    class ExtensionStorage {
        private val _registeredExtensions = mutableMapOf<ProjectExtensionDescriptor<*>, MutableList<Any>>()
        val registeredExtensions: Map<ProjectExtensionDescriptor<*>, List<Any>>
            get() = _registeredExtensions

        private val _disposables = mutableListOf<PluginDisposable>()
        val disposables: List<PluginDisposable>
            get() = _disposables

        fun <T : Any> ProjectExtensionDescriptor<T>.registerExtension(extension: T) {
            _registeredExtensions.getOrPut(this, ::mutableListOf).add(extension)
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
            _disposables += disposable
        }
    }

    fun interface PluginDisposable {
        fun dispose()
    }

    abstract val supportsK2: Boolean
}

fun CompilerPluginRegistrar.ExtensionStorage.registerInProject(
    project: Project,
    errorMessage: (Any) -> String = { "Error while registering ${it.javaClass.name} "}
) {
    for ((extensionPoint, extensions) in registeredExtensions) {
        for (extension in extensions) {
            @Suppress("UNCHECKED_CAST")
            try {
                (extensionPoint as ProjectExtensionDescriptor<Any>).registerExtensionUnsafe(project, extension)
            } catch (e: AbstractMethodError) {
                throw IllegalStateException(errorMessage(extension), e)
            }
        }
    }
    for (disposable in disposables) {
        Disposer.register(project) { disposable.dispose() }
    }
}

private fun ProjectExtensionDescriptor<Any>.registerExtensionUnsafe(project: Project, extension: Any) {
    this.registerExtension(project, extension)
}

@TestOnly
fun registerExtensionsForTest(
    project: Project,
    configuration: CompilerConfiguration,
    register: CompilerPluginRegistrar.ExtensionStorage.(CompilerConfiguration) -> Unit
) {
    val extensionStorage = CompilerPluginRegistrar.ExtensionStorage().apply {
        register(configuration)
    }
    extensionStorage.registerInProject(project)
}

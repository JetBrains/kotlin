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

    /**
     * Uniquely identifies the Kotlin compiler plugin. Should match the `pluginId` specified in [CommandLineProcessor].
     * The same string can be used by other compiler plugins to indicate a dependency on the functionality of this compiler plugin.
     * An empty string (default value) is used to indicate this compiler plugin has no ID.
     */
    open val pluginId: String
        get() = ""

    /**
     * Indicates the [pluginId]s for the dependencies of this compiler plugin.
     * It does not indicate a requirement on the dependency plugin being applied to the compilation.
     * This property is only used to topologically sort plugin registration,
     * thus also sorting plugin extension execution.
     */
    open val dependencyPluginIds: Set<String>
        get() = emptySet()

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

/**
 * This configuration key is used to provide a way to programmatically register compiler plugins
 * in the [org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.registerExtensionsFromPlugins].
 *
 * This key is allowed to be used ONLY in tests
 */
val TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK: CompilerConfigurationKey<(Project) -> Unit> =
    CompilerConfigurationKey.create("Compiler plugin registrars for tests")

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

fun CompilerPluginRegistrar.ExtensionStorage.registerInProject(
    project: Project,
    errorMessage: (Any) -> String = { "Error while registering ${it.javaClass.name} "}
) {
    for ((extensionPoint, extensions) in registeredExtensions) {
        for (extension in extensions) {
            @Suppress("UNCHECKED_CAST")
            try {
                if (extensionPoint is ProjectExtensionDescriptor<*>) {
                    (extensionPoint as ProjectExtensionDescriptor<Any>).registerExtensionUnsafe(project, extension)
                }
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
    val extensionStorage = configuration.extensionsStorage ?: error("Extensions storage is not registered")
    extensionStorage.register(configuration)
    extensionStorage.registerInProject(project)
}

/**
 * This configuration key is used to provide a way to programmatically register compiler plugins
 * in the [org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.registerExtensionsFromPlugins].
 *
 * This key is allowed to be used ONLY in tests
 */
val TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK: CompilerConfigurationKey<(CompilerPluginRegistrar.ExtensionStorage) -> Unit> =
    CompilerConfigurationKey.create("TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK")

/**
 * This configuration key is used to provide a way to programmatically do something with [Project]
 * during environment configuration.
 *
 * This key is allowed to be used ONLY in tests
 */
val TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK: CompilerConfigurationKey<(Project) -> Unit> =
    CompilerConfigurationKey.create("TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK")

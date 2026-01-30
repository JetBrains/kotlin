/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage

/**
 * Default way for creating `CompilerConfiguration`.
 * Creates a new configuration and registers default services.
 */
fun CompilerConfiguration.Companion.create(): CompilerConfiguration {
    @OptIn(CompilerConfiguration.Internals::class)
    return CompilerConfiguration().apply {
        registerExtensionStorage()
        initializeDiagnosticFactoriesStorageForCli()
    }
}

fun CompilerConfiguration.registerExtensionStorage() {
    extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
}

fun CompilerConfiguration.initializeDiagnosticFactoriesStorageForCli() {
    val storage = KtRegisteredDiagnosticFactoriesStorage()
    storage.registerDiagnosticContainers(CliDiagnostics)
    this.diagnosticFactoriesStorage = storage
}

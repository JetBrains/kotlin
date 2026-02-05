/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl

/**
 * Default way for creating `CompilerConfiguration`.
 * Creates a new configuration and registers default services.
 *
 * If [diagnosticsCollector] is not provided, a new [DiagnosticsCollectorImpl] is created.
 */
@JvmOverloads
fun CompilerConfiguration.Companion.create(
    diagnosticsCollector: BaseDiagnosticsCollector? = null,
    messageCollector: MessageCollector? = null,
): CompilerConfiguration {
    @OptIn(CompilerConfiguration.Internals::class)
    return CompilerConfiguration().apply {
        registerExtensionStorage()
        initializeDiagnosticFactoriesStorageForCli()
        this.diagnosticsCollector = diagnosticsCollector ?: DiagnosticsCollectorImpl()
        messageCollector?.let { this.messageCollector = it }
    }
}

@CompilerConfiguration.Internals("Consider using `CompilerConfiguration.Companion.create()` which registers default services")
fun CompilerConfiguration.registerExtensionStorage() {
    extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
}

private fun CompilerConfiguration.initializeDiagnosticFactoriesStorageForCli() {
    val storage = KtRegisteredDiagnosticFactoriesStorage()
    storage.registerDiagnosticContainers(CliDiagnostics)
    this.diagnosticFactoriesStorage = storage
}

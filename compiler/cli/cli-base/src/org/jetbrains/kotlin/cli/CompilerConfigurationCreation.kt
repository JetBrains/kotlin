/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.diagnostics.LightTreeParseTreeKind
import org.jetbrains.kotlin.diagnostics.ParseTreeKind
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl

val DEFAULT_PARSE_TREE_KIND: ParseTreeKind = LightTreeParseTreeKind

/**
 * Default way for creating `CompilerConfiguration`.
 * Creates a new configuration and registers default services.
 *
 * If [diagnosticsCollector] is not provided, a new [DiagnosticsCollectorImpl] is created.
 * If [parseTreeKind] is not provided, [DEFAULT_PARSE_TREE_KIND] is used.
 */
@JvmOverloads
fun CompilerConfiguration.Companion.create(
    diagnosticsCollector: BaseDiagnosticsCollector? = null,
    messageCollector: MessageCollector? = null,
    parseTreeKind: ParseTreeKind = DEFAULT_PARSE_TREE_KIND,
): CompilerConfiguration {
    @OptIn(CompilerConfiguration.Internals::class)
    return CompilerConfiguration().apply {
        registerExtensionStorage()
        initializeDiagnosticFactoriesStorageForCli()
        this.diagnosticsCollector = diagnosticsCollector ?: DiagnosticsCollectorImpl()
        @OptIn(MessageCollectorAccess::class) // write access
        messageCollector?.let { this.messageCollector = it }
        this.parseTreeKind = parseTreeKind
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

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.cli

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.syntax.ParseTreeKind

object FrontendConfigurationKeys {
    // Container of all registered diagnostic factories.
    @JvmField
    val DIAGNOSTIC_FACTORIES_STORAGE = CompilerConfigurationKey.create<KtRegisteredDiagnosticFactoriesStorage>("DIAGNOSTIC_FACTORIES_STORAGE")

    // The kind of parse tree used in the compilation (PSI, LightTree or KMP). It's also used to position diagnostics.
    @JvmField
    val PARSE_TREE_KIND = CompilerConfigurationKey.create<ParseTreeKind>("PARSE_TREE_KIND")

    // Storage of registered compiler plugins.
    @JvmField
    @OptIn(ExperimentalCompilerApi::class)
    val EXTENSIONS_STORAGE = CompilerConfigurationKey.create<ExtensionStorage>("EXTENSIONS_STORAGE")

}

var CompilerConfiguration.diagnosticFactoriesStorage: KtRegisteredDiagnosticFactoriesStorage?
    get() = get(FrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE)
    set(value) { put(FrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.parseTreeKind: ParseTreeKind
    get() = getOrDefault(FrontendConfigurationKeys.PARSE_TREE_KIND) { error("parse tree kind is not initialized") }
    set(value) { put(FrontendConfigurationKeys.PARSE_TREE_KIND, value) }

@OptIn(ExperimentalCompilerApi::class)
var CompilerConfiguration.extensionsStorage: ExtensionStorage?
    get() = get(FrontendConfigurationKeys.EXTENSIONS_STORAGE)
    set(value) { put(FrontendConfigurationKeys.EXTENSIONS_STORAGE, requireNotNull(value) { "nullable values are not allowed" }) }


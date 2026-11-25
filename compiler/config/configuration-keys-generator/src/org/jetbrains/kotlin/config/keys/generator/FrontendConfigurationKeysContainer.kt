/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.syntax.ParseTreeKind

@Suppress("unused")
object FrontendConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.cli", "FrontendConfigurationKeys") {
    val DIAGNOSTIC_FACTORIES_STORAGE by key<KtRegisteredDiagnosticFactoriesStorage>("Container of all registered diagnostic factories.")

    val PARSE_TREE_KIND by key<ParseTreeKind>(
        "The kind of parse tree used in the compilation (PSI, LightTree or KMP). It's also used to position diagnostics.",
        lazyDefaultValue = """error("parse tree kind is not initialized")""",
    )

    @OptIn(ExperimentalCompilerApi::class)
    val EXTENSIONS_STORAGE by key<CompilerPluginRegistrar.ExtensionStorage>(
        "Storage of registered compiler plugins.",
        optIns = listOf(ExperimentalCompilerApi())
    )
}

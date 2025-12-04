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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage

object FrontendConfigurationKeys {
    @JvmField
    val DIAGNOSTIC_FACTORIES_STORAGE = CompilerConfigurationKey.create<KtRegisteredDiagnosticFactoriesStorage>("Container of all registered diagnostic factories")

}

var CompilerConfiguration.diagnosticFactoriesStorage: KtRegisteredDiagnosticFactoriesStorage?
    get() = get(FrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE)
    set(value) { put(FrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE, requireNotNull(value) { "nullable values are not allowed" }) }


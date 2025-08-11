/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.FrontendJsSymbols
import org.jetbrains.kotlin.backend.common.ir.FrontendJsSymbolsImpl
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.types.Variance

class JsPreSerializationLoweringContext(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    diagnosticReporter: DiagnosticReporter,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {
    override val symbols: FrontendJsSymbols by lazy {
        FrontendJsSymbolsImpl(irBuiltIns)
    }

    override val sharedVariablesManager by lazy { KlibSharedVariablesManager(symbols) }
}

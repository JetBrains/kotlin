/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.FrontendNativeSymbols
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns

class NativePreSerializationLoweringContext(
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        diagnosticReporter: DiagnosticReporter,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {
    override val symbols: FrontendNativeSymbols = FrontendNativeSymbols.FrontendNativeSymbolsImpl(irBuiltIns)

    override val sharedVariablesManager = KlibSharedVariablesManager(symbols)
}

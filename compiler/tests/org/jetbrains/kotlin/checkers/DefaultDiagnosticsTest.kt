/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.junit.Test

class DefaultDiagnosticsTest {
    @Test
    fun verify() {
        verifyDiagnostics(
            FirErrors,
            FirJvmErrors,
            FirJsErrors,
            FirNativeErrors,
            FirWasmErrors,
            FirWebCommonErrors,
            FirSyntaxErrors,
            CliDiagnostics
        )
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizationErrors
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationErrors
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliFrontendDiagnostics
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibErrors
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmKlibErrors
import org.jetbrains.kotlin.ir.inline.diagnostics.IrInlinerErrors
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
            CliDiagnostics,
            CliFrontendDiagnostics,
            JsKlibErrors,
            JvmBackendErrors,
            CommonBackendErrors,
            IrActualizationErrors,
            IrInlinerErrors,
            PartialLinkageDiagnostics,
            SerializationErrors,
            WasmKlibErrors,
        )
    }
}

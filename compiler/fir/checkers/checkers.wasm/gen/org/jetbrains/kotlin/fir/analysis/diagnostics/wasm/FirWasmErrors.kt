/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.wasm

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirWasmErrors {
    // Externals
    val NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWasmErrorsDefaultMessages)
    }
}

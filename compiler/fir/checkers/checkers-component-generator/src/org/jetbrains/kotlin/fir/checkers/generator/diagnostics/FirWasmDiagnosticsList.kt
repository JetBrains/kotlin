/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.PrivateForInline

@Suppress("ClassName", "unused")
@OptIn(PrivateForInline::class)
object WASM_DIAGNOSTICS_LIST : DiagnosticList("FirWasmErrors") {
    val EXTERNALS by object : DiagnosticGroup("Externals") {
        val NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<ConeKotlinType>("superType")
        }
        val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<ConeKotlinType>("superType")
        }
    }
}

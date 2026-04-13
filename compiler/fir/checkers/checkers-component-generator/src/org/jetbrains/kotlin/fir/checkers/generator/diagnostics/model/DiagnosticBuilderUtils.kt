/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import org.jetbrains.kotlin.fir.types.ConeKotlinType

internal fun upperBoundViolatedDiagnosticInit(withExtraMessage: Boolean = false): DiagnosticBuilder.() -> Unit = {
    parameter<ConeKotlinType>("expectedUpperBound")
    parameter<ConeKotlinType>("actualType")
    parameter<ConeKotlinType>("onTypeParameter")
    if (withExtraMessage) {
        parameter<String>("extraMessage")
    }
}

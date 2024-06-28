/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

interface ConeDiagnostic {
    val reason: String
}

interface ConeDiagnosticWithNullability : ConeDiagnostic {
    val isNullable: Boolean
}

/**
 * A [ConeDiagnostic] that is never reported.
 * It is used when multiple FIR nodes, like a type ref and a reference, would otherwise contain the same diagnostic,
 * which would lead to duplicate diagnostics being reported.
 *
 * Call sites should document which FIR element's diagnostic this is duplicating and why the usage won't lead to missed diagnostics.
 */
class ConeUnreportedDuplicateDiagnostic(val original: ConeDiagnostic) : ConeDiagnostic {
    override val reason: String get() = original.reason
}
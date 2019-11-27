/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

abstract class FirDiagnostic {
    abstract val reason: String
}

class FirStubDiagnostic(val original: FirDiagnostic) : FirDiagnostic() {
    override val reason: String get() = original.reason
}
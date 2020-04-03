/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

abstract class ConeDiagnostic {
    abstract val reason: String
}

class ConeStubDiagnostic(val original: ConeDiagnostic) : ConeDiagnostic() {
    override val reason: String get() = original.reason
}
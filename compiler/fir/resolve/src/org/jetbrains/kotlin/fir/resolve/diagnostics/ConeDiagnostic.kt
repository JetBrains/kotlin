/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import org.jetbrains.kotlin.fir.FirSourceElement

class ConeDiagnosticFactory {
    lateinit var name: String

    fun on(source: FirSourceElement): ConeDiagnostic = ConeDiagnostic(this, source)
}

class ConeDiagnostic(
    val factory: ConeDiagnosticFactory,
    val source: FirSourceElement
)
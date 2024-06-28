/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.type.FirDynamicUnsupportedChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers

object WasmBaseTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker>
        get() = setOf(
            FirDynamicUnsupportedChecker,
        )
}

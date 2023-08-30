/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration.*

object WasmDeclarationCheckers : DeclarationCheckers() {
    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
        )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirWasmExternalInheritanceChecker
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
        )
}

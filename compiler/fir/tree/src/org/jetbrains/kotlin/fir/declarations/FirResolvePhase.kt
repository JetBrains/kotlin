/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirResolvePhase(val noProcessor: Boolean = false) {
    RAW_FIR(noProcessor = true),
    IMPORTS,
    COMPILER_REQUIRED_ANNOTATIONS,
    COMPANION_GENERATION,
    SUPER_TYPES,
    SEALED_CLASS_INHERITORS,
    TYPES,
    STATUS,
    EXPECT_ACTUAL_MATCHING,
    ARGUMENTS_OF_ANNOTATIONS,
    CONTRACTS,
    IMPLICIT_TYPES_BODY_RESOLVE,
    ANNOTATIONS_ARGUMENTS_MAPPING,
    BODY_RESOLVE;

    val next: FirResolvePhase get() = values()[ordinal + 1]
    val previous: FirResolvePhase get() = values()[ordinal - 1]

    companion object {
        // Short-cut
        val DECLARATIONS = STATUS
        val ANALYZED_DEPENDENCIES = BODY_RESOLVE
    }
}

val FirResolvePhase.isBodyResolve: Boolean
    get() = when (this) {
        FirResolvePhase.BODY_RESOLVE,
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> true
        else -> false
    }

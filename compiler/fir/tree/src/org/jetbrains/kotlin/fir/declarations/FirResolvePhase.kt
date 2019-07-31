/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirResolvePhase {
    RAW_FIR,
    IMPORTS,
    SUPER_TYPES,
    TYPES,
    STATUS,
    IMPLICIT_TYPES_BODY_RESOLVE,
    BODY_RESOLVE;

    val prev: FirResolvePhase get() = values()[ordinal - 1]

    val next: FirResolvePhase get() = values()[ordinal + 1]

    companion object {
        // Short-cut
        val DECLARATIONS = STATUS
    }
}
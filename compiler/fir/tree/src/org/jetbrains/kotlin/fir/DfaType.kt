/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/**
 * A generalization over [ConeKotlinType] and literal types for the purposes of DFA
 * (specifically, `when` exhaustiveness checking).
 * [DfaType] exists because introducing a fully fledged [ConeKotlinType] is
 * overkill for some types due to their limited support in the compiler.
 */
sealed class DfaType {
    /**
     * The embedding of the usual [ConeKotlinType] into [DfaType].
     */
    data class Cone(val type: ConeKotlinType) : DfaType() {
        override fun toString(): String = "$type"
    }

    /**
     * A representation of a literal type corresponding to a singleton declaration
     * (such as an enum entry or an object).
     */
    data class Symbol(val symbol: FirBasedSymbol<*>) : DfaType() {
        override fun toString(): String = "$symbol"
    }

    /**
     * A representation of the types corresponding to `true` and `false`.
     */
    data class BooleanLiteral(val value: Boolean) : DfaType() {
        override fun toString(): String = "$value"
    }
}

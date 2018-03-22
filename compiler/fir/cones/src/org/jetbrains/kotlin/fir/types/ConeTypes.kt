/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol

sealed class ConeKotlinTypeProjection(val kind: ProjectionKind)

enum class ProjectionKind {
    STAR, IN, OUT, INVARIANT
}

object StarProjection : ConeKotlinTypeProjection(ProjectionKind.STAR)

abstract class ConeKotlinTypeProjectionIn : ConeKotlinTypeProjection(ProjectionKind.IN) {
    abstract val type: ConeKotlinType
}

abstract class ConeKotlinTypeProjectionOut : ConeKotlinTypeProjection(ProjectionKind.OUT) {
    abstract val type: ConeKotlinType
}

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(ProjectionKind.INVARIANT)

class ConeKotlinErrorType(val reason: String) : ConeKotlinType() {
    override fun toString(): String {
        return "<ERROR TYPE: $reason>"
    }
}

class ConeClassErrorType(val reason: String): ConeClassLikeType() {
    override val symbol: ConeClassLikeSymbol
        get() = error("!")
    override val typeArguments: List<ConeKotlinTypeProjection>
        get() = emptyList()

}

sealed class ConeSymbolBasedType : ConeKotlinType() {
    abstract val symbol: ConeSymbol
}

abstract class ConeClassLikeType : ConeSymbolBasedType() {
    abstract override val symbol: ConeClassLikeSymbol

    abstract val typeArguments: List<ConeKotlinTypeProjection>
}

abstract class ConeAbbreviatedType : ConeClassLikeType() {
    abstract val abbreviationSymbol: ConeClassLikeSymbol

    abstract val directExpansion: ConeClassLikeType
}

abstract class ConeTypeParameterType : ConeSymbolBasedType() {
    abstract override val symbol: ConeTypeParameterSymbol
}
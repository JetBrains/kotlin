/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.model.TypeArgumentMarker

enum class ProjectionKind {
    STAR, IN, OUT, INVARIANT;
}

sealed class ConeTypeProjection : TypeArgumentMarker {
    abstract val kind: ProjectionKind

    companion object {
        val EMPTY_ARRAY = arrayOf<ConeTypeProjection>()
    }
}

object ConeStarProjection : ConeTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.STAR
}

sealed class ConeKotlinTypeProjection : ConeTypeProjection() {
    abstract val type: ConeKotlinType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeKotlinTypeProjection) return false

        if (kind != other.kind) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode() * 31 + kind.hashCode()
    }
}

data class ConeKotlinTypeProjectionIn(override val type: ConeKotlinType) : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.IN
}

data class ConeKotlinTypeProjectionOut(override val type: ConeKotlinType) : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.OUT
}

data class ConeKotlinTypeConflictingProjection(override val type: ConeKotlinType) : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT
}

val ConeTypeProjection.type: ConeKotlinType?
    get() = when (this) {
        ConeStarProjection -> null
        is ConeKotlinTypeProjection -> type
    }

val ConeTypeProjection.isStarProjection: Boolean
    get() = this == ConeStarProjection

fun ConeTypeProjection.replaceType(newType: ConeKotlinType?): ConeTypeProjection {
    return when (this) {
        is ConeStarProjection -> this
        is ConeKotlinTypeProjection -> {
            requireNotNull(newType) { "Type for non star projection should be not null" }
            replaceType(newType)
        }
    }
}

fun ConeKotlinTypeProjection.replaceType(newType: ConeKotlinType): ConeKotlinTypeProjection {
    return when (this) {
        is ConeKotlinType -> newType
        is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
        is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
        is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(newType)
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.*

// ----------------------------------- Type variable type -----------------------------------

class ConeTypeVariableType(
    val isMarkedNullable: Boolean,
    val typeConstructor: ConeTypeVariableTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleKotlinType() {
    override val typeArguments: Array<out ConeTypeProjection> get() = EMPTY_ARRAY
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableType) return false

        if (isMarkedNullable != other.isMarkedNullable) return false
        if (typeConstructor != other.typeConstructor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + typeConstructor.hashCode()
        return result
    }
}

class ConeTypeVariableTypeConstructor(
    val debugName: String,
    val originalTypeParameter: TypeParameterMarker?
) : TypeVariableTypeConstructorMarker, ConeTypeConstructorMarker {
    val name: Name get() = Name.identifier(debugName)

    var isContainedInInvariantOrContravariantPositions: Boolean = false
        private set

    fun recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter() {
        isContainedInInvariantOrContravariantPositions = true
    }

    override fun toString(): String = "${this::class.simpleName}($debugName)"
}

// ----------------------------------- Stub types -----------------------------------

data class ConeStubTypeConstructor(
    val variable: ConeTypeVariable,
    val isTypeVariableInSubtyping: Boolean,
    val isForFixation: Boolean = false,
) : ConeTypeConstructorMarker {
    override fun toString(): String {
        return "Stub(${variable.typeConstructor.debugName})"
    }
}

sealed class ConeStubType(
    val constructor: ConeStubTypeConstructor,
    val isMarkedNullable: Boolean,
) : StubTypeMarker,
    ConeSimpleKotlinType() {

    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeStubType

        if (constructor != other.constructor) return false
        if (isMarkedNullable != other.isMarkedNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + constructor.hashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        return result
    }
}

class ConeStubTypeForTypeVariableInSubtyping(
    constructor: ConeStubTypeConstructor,
    isMarkedNullable: Boolean
) : ConeStubType(constructor, isMarkedNullable) {
    constructor(variable: ConeTypeVariable, isMarkedNullable: Boolean) : this(
        ConeStubTypeConstructor(
            variable,
            isTypeVariableInSubtyping = true
        ), isMarkedNullable
    )
}

open class ConeTypeVariable(name: String, originalTypeParameter: TypeParameterMarker? = null) : TypeVariableMarker {
    val typeConstructor: ConeTypeVariableTypeConstructor = ConeTypeVariableTypeConstructor(name, originalTypeParameter)
    val defaultType: ConeTypeVariableType = ConeTypeVariableType(isMarkedNullable = false, typeConstructor)

    override fun toString(): String {
        return defaultType.toString()
    }
}

/**
 * Make a transformation from marker interface to cone-based type
 *
 * In K2 frontend context such a transformation is normally safe,
 * as K1-based types and IR-based types cannot occur here.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun TypeVariableMarker.asCone(): ConeTypeVariable = this as ConeTypeVariable

@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeTypeVariable.asCone(): ConeTypeVariable = this
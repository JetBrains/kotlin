/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.*

// ----------------------------------- Type variable type -----------------------------------

class ConeTypeVariableType(
    override val nullability: ConeNullability,
    val typeConstructor: ConeTypeVariableTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleKotlinType() {
    override val typeArguments: Array<out ConeTypeProjection> get() = EMPTY_ARRAY
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableType) return false

        if (nullability != other.nullability) return false
        if (typeConstructor != other.typeConstructor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + nullability.hashCode()
        result = 31 * result + typeConstructor.hashCode()
        return result
    }
}

class ConeTypeVariableTypeConstructor(
    val debugName: String,
    val originalTypeParameter: TypeParameterMarker?
) : TypeVariableTypeConstructorMarker {
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
) : TypeConstructorMarker {
    override fun toString(): String {
        return "Stub(${variable.typeConstructor.debugName})"
    }
}

sealed class ConeStubType(val constructor: ConeStubTypeConstructor, override val nullability: ConeNullability) : StubTypeMarker,
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
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + constructor.hashCode()
        result = 31 * result + nullability.hashCode()
        return result
    }
}

open class ConeStubTypeForChainInference(
    constructor: ConeStubTypeConstructor,
    nullability: ConeNullability
) : ConeStubType(constructor, nullability) {
    constructor(variable: ConeTypeVariable, nullability: ConeNullability) : this(
        ConeStubTypeConstructor(
            variable,
            isTypeVariableInSubtyping = false
        ), nullability
    )
}

class ConeStubTypeForTypeVariableInSubtyping(
    constructor: ConeStubTypeConstructor,
    nullability: ConeNullability
) : ConeStubType(constructor, nullability) {
    constructor(variable: ConeTypeVariable, nullability: ConeNullability) : this(
        ConeStubTypeConstructor(
            variable,
            isTypeVariableInSubtyping = true
        ), nullability
    )
}

open class ConeTypeVariable(name: String, originalTypeParameter: TypeParameterMarker? = null) : TypeVariableMarker {
    val typeConstructor = ConeTypeVariableTypeConstructor(name, originalTypeParameter)
    val defaultType = ConeTypeVariableType(ConeNullability.NOT_NULL, typeConstructor)

    override fun toString(): String {
        return defaultType.toString()
    }
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.StubTypeMarker

class StubTypeForBuilderInference(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForBuilderInference(originalTypeVariable, newNullability, constructor)

    override val memberScope = originalTypeVariable.builtIns.anyType.memberScope

    override fun toString(): String {
        // BI means builder inference
        return "Stub (BI): $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForTypeVariablesInSubtyping(originalTypeVariable, newNullability, constructor)

    override fun toString(): String {
        return "Stub (subtyping): $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }
}

// This type is used as a replacement of type variables for provideDelegate resolve
class StubTypeForProvideDelegateReceiver(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable) {
    override fun materialize(newNullability: Boolean): StubTypeForProvideDelegateReceiver =
        StubTypeForProvideDelegateReceiver(originalTypeVariable, newNullability, constructor)

    override fun toString(): String {
        return "Stub (delegation): $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }
}

abstract class AbstractStubType(val originalTypeVariable: TypeConstructor, override val isMarkedNullable: Boolean) : SimpleType() {
    override val memberScope = ErrorUtils.createErrorScope("Scope for stub type: $originalTypeVariable")

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedNullable) this else materialize(newNullability)
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this

    abstract fun materialize(newNullability: Boolean): AbstractStubType

    companion object {
        fun createConstructor(originalTypeVariable: TypeConstructor) =
            ErrorUtils.createErrorTypeConstructor("Constructor for stub type: $originalTypeVariable")
    }
}

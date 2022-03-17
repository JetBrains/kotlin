/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.error.ErrorUtils

class StubTypeForBuilderInference(
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForBuilderInference(originalTypeVariable, newNullability, constructor)

    override val memberScope: MemberScope = originalTypeVariable.builtIns.anyType.memberScope

    override fun toString(): String {
        // BI means builder inference
        return "Stub (BI): $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: NewTypeVariableConstructor,
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
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable) {
    override fun materialize(newNullability: Boolean): StubTypeForProvideDelegateReceiver =
        StubTypeForProvideDelegateReceiver(originalTypeVariable, newNullability, constructor)

    override fun toString(): String {
        return "Stub (delegation): $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }
}

abstract class AbstractStubType(val originalTypeVariable: NewTypeVariableConstructor, override val isMarkedNullable: Boolean) : SimpleType() {
    override val memberScope: MemberScope = ErrorUtils.createErrorScope(ErrorScopeKind.STUB_TYPE_SCOPE, originalTypeVariable.toString())

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedNullable) this else materialize(newNullability)
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this

    abstract fun materialize(newNullability: Boolean): AbstractStubType

    companion object {
        fun createConstructor(originalTypeVariable: NewTypeVariableConstructor) =
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.STUB_TYPE, originalTypeVariable.toString())
    }
}

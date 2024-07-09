/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.*

class ClassicTypeSystemContextForCS(
    override val builtIns: KotlinBuiltIns,
    val kotlinTypeRefiner: KotlinTypeRefiner
) : TypeSystemInferenceExtensionContextDelegate,
    ClassicTypeSystemContext,
    BuiltInsProvider {

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.defaultType
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.freshTypeConstructor
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus,
        identity: CapturedTypeConstructorMarker?
    ): CapturedTypeMarker {
        require(lowerType is UnwrappedType?, lowerType::errorMessage)
        require(constructorProjection is TypeProjectionBase, constructorProjection::errorMessage)

        @Suppress("UNCHECKED_CAST")
        val newCapturedTypeConstructor = NewCapturedTypeConstructor(
            constructorProjection,
            constructorSupertypes as List<UnwrappedType>
        )
        return NewCapturedType(
            captureStatus,
            newCapturedTypeConstructor,
            lowerType = lowerType
        )
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        if (map.isEmpty()) return createEmptySubstitutor()
        @Suppress("UNCHECKED_CAST")
        return NewTypeSubstitutorByConstructorMap(map as Map<TypeConstructor, UnwrappedType>)
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return EmptySubstitutor
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(type is KotlinType, type::errorMessage)
        val unwrappedType = type.unwrap()
        return when (this) {
            is NewTypeSubstitutor -> safeSubstitute(unwrappedType)
            is TypeSubstitutor -> safeSubstitute(unwrappedType, Variance.INVARIANT)
            else -> error(this.errorMessage())
        }
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForBuilderInference(
            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
            typeVariable.defaultType().isMarkedNullable()
        )
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForTypeVariablesInSubtyping(
            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
            typeVariable.defaultType().isMarkedNullable()
        )
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is TypeVariableTypeConstructor
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        require(this is TypeVariableTypeConstructor)
        return isContainedInInvariantOrContravariantPositions
    }

    override fun newTypeCheckerState(errorTypesEqualToAnything: Boolean, stubTypesEqualToAnything: Boolean): TypeCheckerState {
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this,
            kotlinTypeRefiner = kotlinTypeRefiner
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Any?.errorMessage(): String {
    return "ClassicTypeSystemContextForCS couldn't handle: $this, ${this?.let { it::class }}"
}

@Suppress("FunctionName")
fun NewConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: KotlinBuiltIns,
    kotlinTypeRefiner: KotlinTypeRefiner,
    languageVersionSettings: LanguageVersionSettings
): NewConstraintSystemImpl {
    return NewConstraintSystemImpl(constraintInjector, ClassicTypeSystemContextForCS(builtIns, kotlinTypeRefiner), languageVersionSettings)
}

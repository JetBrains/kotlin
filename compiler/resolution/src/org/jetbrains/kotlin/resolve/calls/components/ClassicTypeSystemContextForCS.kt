/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.types.StubType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.ClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ClassicTypeSystemContextForCS(override val builtIns: KotlinBuiltIns) : TypeSystemInferenceExtensionContextDelegate,
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
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is UnwrappedType?, lowerType::errorMessage)
        require(constructorProjection is TypeProjectionImpl, constructorProjection::errorMessage)

        @Suppress("UNCHECKED_CAST")
        val newCapturedTypeConstructor = NewCapturedTypeConstructor(
            constructorProjection,
            constructorSupertypes as List<UnwrappedType>
        )
        return NewCapturedType(
            CaptureStatus.FOR_INCORPORATION,
            newCapturedTypeConstructor,
            lowerType = lowerType
        )
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        return NewTypeSubstitutorByConstructorMap(map.cast())
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(type is UnwrappedType, this::errorMessage)
        require(this is NewTypeSubstitutor, this::errorMessage)
        return this.safeSubstitute(type)
    }

    override fun createStubType(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubType(typeVariable.freshTypeConstructor() as TypeConstructor, typeVariable.defaultType().isMarkedNullable())
    }
}



@Suppress("NOTHING_TO_INLINE")
private inline fun Any?.errorMessage(): String {
    return "ClassicTypeSystemContextForCS couldn't handle: $this, ${this?.let { it::class }}"
}

@Suppress("FunctionName")
fun NewConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: KotlinBuiltIns
): NewConstraintSystemImpl {
    return NewConstraintSystemImpl(constraintInjector, ClassicTypeSystemContextForCS(builtIns))
}
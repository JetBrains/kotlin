/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructorImpl
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.typeUtil.makeNullable

@DefaultImplementation(impl = KotlinTypePreparator.Default::class)
abstract class KotlinTypePreparator : AbstractTypePreparator() {
    private fun transformToNewType(type: SimpleType): SimpleType {
        when (val constructor = type.constructor) {
            // Type itself can be just SimpleTypeImpl, not CapturedType. see KT-16147
            is CapturedTypeConstructorImpl -> {
                val lowerType = constructor.projection.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()

                // it is incorrect calculate this type directly because of recursive star projections
                if (constructor.newTypeConstructor == null) {
                    constructor.newTypeConstructor =
                        NewCapturedTypeConstructor(constructor.projection, constructor.supertypes.map { it.unwrap() })
                }
                return NewCapturedType(
                    CaptureStatus.FOR_SUBTYPING, constructor.newTypeConstructor!!,
                    lowerType, type.annotations, type.isMarkedNullable
                )
            }

            is IntegerValueTypeConstructor -> {
                val newConstructor =
                    IntersectionTypeConstructor(constructor.supertypes.map { TypeUtils.makeNullableAsSpecified(it, type.isMarkedNullable) })
                return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                    type.annotations,
                    newConstructor,
                    listOf(),
                    false,
                    type.memberScope
                )
            }

            is IntersectionTypeConstructor -> if (type.isMarkedNullable) {
                val newConstructor = constructor.transformComponents(transform = { it.makeNullable() }) ?: constructor
                return newConstructor.createType()

            }
        }

        return type
    }

    override fun prepareType(type: KotlinTypeMarker): UnwrappedType {
        require(type is KotlinType)
        val unwrappedType = type.unwrap()
        return when (unwrappedType) {
            is SimpleType -> transformToNewType(unwrappedType)
            is FlexibleType -> {
                val newLower = transformToNewType(unwrappedType.lowerBound)
                val newUpper = transformToNewType(unwrappedType.upperBound)
                if (newLower !== unwrappedType.lowerBound || newUpper !== unwrappedType.upperBound) {
                    KotlinTypeFactory.flexibleType(newLower, newUpper)
                } else {
                    unwrappedType
                }
            }
        }.inheritEnhancement(unwrappedType, ::prepareType)
    }

    object Default : KotlinTypePreparator()
}

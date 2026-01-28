/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.model.projection

class ConeTypePreparator(val session: FirSession) : AbstractTypePreparator() {
    private fun <T : ConeRigidType> prepareType(type: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            is ConeClassLikeType -> type.fullyExpandedType(session)
            is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(prepareType(type.original))
            else -> type
        } as T
    }

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        if (type !is ConeKotlinType) {
            throw AssertionError("Unexpected type in ConeTypePreparator: ${this::class.java}")
        }
        return when (type) {
            is ConeFlexibleType -> type.mapTypesOrSelf(session.typeContext, dropIdentity = true) { prepareType(it) }
            is ConeRigidType -> prepareType(type)
        }
    }

    override fun clearTypeFromUnnecessaryAttributes(type: RigidTypeMarker): RigidTypeMarker {
        return (type as ConeRigidType).dropEnhancedNullability()
    }

    private fun ConeKotlinType.dropEnhancedNullability(): ConeKotlinType {
        when (this) {
            is ConeFlexibleType -> {
                val lowerBound = lowerBound.dropEnhancedNullability()
                val upperBound = upperBound.dropEnhancedNullability()
                if (lowerBound === this.lowerBound && upperBound === this.upperBound) return this
                if (this is ConeRawType) return ConeRawType.create(lowerBound, upperBound)
                return ConeFlexibleType(lowerBound, upperBound, isTrivial = this.isTrivial)
            }
            is ConeIntersectionType -> {
                val intersectedTypes = intersectedTypes.map { it.dropEnhancedNullability() }
                if (intersectedTypes.zip(this.intersectedTypes).all { (a, b) -> a === b }) return this
                return ConeIntersectionType(
                    intersectedTypes,
                    upperBoundForApproximation?.dropEnhancedNullability()
                )
            }
            is ConeCapturedType -> {
                val constructor = constructor
                val lowerType = constructor.lowerType?.dropEnhancedNullability()
                val projection = constructor.projection.dropEnhancedNullability()
                val newConstructor =
                    if (lowerType === constructor.lowerType && projection === constructor.projection) constructor
                    else ConeCapturedTypeConstructor(
                        projection = projection, lowerType = lowerType, captureStatus = constructor.captureStatus
                    )
                val resultCapturedType =
                    if (constructor === newConstructor) this
                    else ConeCapturedType(isMarkedNullable, newConstructor, attributes)
                return resultCapturedType.dropEnhancedNullability()
            }
            is ConeRigidType -> {
                return dropEnhancedNullability()
            }
        }
    }

    private fun ConeRigidType.dropEnhancedNullability(): ConeRigidType {
        return withAttributes(attributes.remove(CompilerConeAttributes.EnhancedNullability))
            .withArguments { projection -> projection.dropEnhancedNullability() }
    }

    private fun ConeTypeProjection.dropEnhancedNullability(): ConeTypeProjection {
        return if (this !is ConeKotlinTypeProjection) this
        else replaceType(type.dropEnhancedNullability())
    }
}

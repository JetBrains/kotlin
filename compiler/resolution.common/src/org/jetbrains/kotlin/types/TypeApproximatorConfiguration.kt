/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.*

open class TypeApproximatorConfiguration {
    enum class IntersectionStrategy {
        ALLOWED,
        TO_FIRST,
        TO_COMMON_SUPERTYPE,
        TO_UPPER_BOUND_IF_SUPERTYPE
    }

    open val flexible: Boolean get() = false // simple flexible types (FlexibleTypeImpl)
    open val dynamic: Boolean get() = false // DynamicType
    open val rawType: Boolean get() = false // RawTypeImpl
    open val errorType: Boolean get() = false
    open val integerLiteralConstantType: Boolean get() = false // IntegerLiteralTypeConstructor
    open val integerConstantOperatorType: Boolean get() = false
    open val definitelyNotNullType: Boolean get() = true
    open val intersection: IntersectionStrategy = IntersectionStrategy.TO_COMMON_SUPERTYPE
    open val intersectionTypesInContravariantPositions = false
    open val localTypes = false

    /**
     * Is only expected to be true for FinalApproximationAfterResolutionAndInference
     * But it's only used for K2 to reproduce K1 behavior for the approximation of resolved calls
     */
    open val convertToNonRawVersionAfterApproximationInK2 get() = false

    /**
     * Whether to approximate anonymous type. This flag does not have any effect if `localTypes` is true because all anonymous types are
     * local.
     */
    open val anonymous = false

    open val typeVariable: (TypeVariableTypeConstructorMarker) -> Boolean = { false }
    open fun capturedType(ctx: TypeSystemInferenceExtensionContext, type: CapturedTypeMarker): Boolean =
        true  // false means that this type we can leave as is

    abstract class AllFlexibleSameValue : TypeApproximatorConfiguration() {
        abstract val allFlexible: Boolean

        override val flexible: Boolean get() = allFlexible
        override val dynamic: Boolean get() = allFlexible
        override val rawType: Boolean get() = allFlexible
    }

    object LocalDeclaration : AllFlexibleSameValue() {
        override val allFlexible: Boolean get() = true
        override val intersection: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val errorType: Boolean get() = true
        override val integerLiteralConstantType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true
    }

    open class PublicDeclaration(override val localTypes: Boolean, override val anonymous: Boolean) : AllFlexibleSameValue() {
        override val allFlexible: Boolean get() = true
        override val errorType: Boolean get() = true
        override val definitelyNotNullType: Boolean get() = false
        override val integerLiteralConstantType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true

        object SaveAnonymousTypes : PublicDeclaration(localTypes = false, anonymous = false)
        object ApproximateAnonymousTypes : PublicDeclaration(localTypes = false, anonymous = true)
    }

    sealed class AbstractCapturedTypesApproximation(val approximatedCapturedStatus: CaptureStatus?) :
        AllFlexibleSameValue() {
        override val allFlexible: Boolean get() = true
        override val errorType: Boolean get() = true

        // i.e. will be approximated only approximatedCapturedStatus captured types
        override fun capturedType(ctx: TypeSystemInferenceExtensionContext, type: CapturedTypeMarker): Boolean =
            approximatedCapturedStatus != null && type.captureStatus(ctx) == approximatedCapturedStatus

        override val intersection: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val typeVariable: (TypeVariableTypeConstructorMarker) -> Boolean get() = { true }
    }

    object IncorporationConfiguration : AbstractCapturedTypesApproximation(CaptureStatus.FOR_INCORPORATION)
    object SubtypeCapturedTypesApproximation : AbstractCapturedTypesApproximation(CaptureStatus.FOR_SUBTYPING)
    object InternalTypesApproximation : AbstractCapturedTypesApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val integerLiteralConstantType: Boolean get() = true
        override val integerConstantOperatorType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true
    }

    object FinalApproximationAfterResolutionAndInference :
        AbstractCapturedTypesApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val integerLiteralConstantType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true

        override val convertToNonRawVersionAfterApproximationInK2: Boolean get() = true
    }

    object TypeArgumentApproximation : AbstractCapturedTypesApproximation(null) {
        override val integerLiteralConstantType: Boolean get() = true
        override val integerConstantOperatorType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true
    }

    object IntegerLiteralsTypesApproximation : AllFlexibleSameValue() {
        override val integerLiteralConstantType: Boolean get() = true
        override val allFlexible: Boolean get() = true
        override val intersection: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val typeVariable: (TypeVariableTypeConstructorMarker) -> Boolean get() = { true }
        override val errorType: Boolean get() = true

        override fun capturedType(ctx: TypeSystemInferenceExtensionContext, type: CapturedTypeMarker): Boolean = false
    }

    object UpperBoundAwareIntersectionTypeApproximator : AllFlexibleSameValue() {
        override val allFlexible: Boolean get() = true
        override val intersection: IntersectionStrategy = IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE
    }
}

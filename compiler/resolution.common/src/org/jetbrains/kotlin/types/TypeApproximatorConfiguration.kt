/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.types.model.*

abstract class TypeApproximatorConfiguration {
    enum class IntersectionStrategy {
        ALLOWED,
        TO_FIRST,
        TO_COMMON_SUPERTYPE,

        @AllowedToUsedOnlyInK1
        TO_UPPER_BOUND_IF_SUPERTYPE
    }

    // Currently, it's only `true` for sone analysis API configuration
    // at org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator.PublicApproximatorConfiguration
    // and at org.jetbrains.kotlin.analysis.api.descriptors.utils.PublicApproximatorConfiguration
    protected abstract val approximateAllFlexible: Boolean

    // Not sure if we should remove them, but at least we see here that
    // they're all always `approximateAllFlexible`
    val approximateFlexible: Boolean get() = approximateAllFlexible
    val approximateDynamic: Boolean get() = approximateAllFlexible
    val approximateRawTypes: Boolean get() = approximateAllFlexible

    open val approximateErrorTypes: Boolean get() = true

    open val approximateIntegerLiteralConstantTypes: Boolean get() = false // IntegerLiteralTypeConstructor
    open val approximateIntegerConstantOperatorTypes: Boolean get() = false
    open val expectedTypeForIntegerLiteralType: KotlinTypeMarker? get() = null

    /**
     * If [LanguageFeature.DefinitelyNonNullableTypes] is enabled, this property is ignored.
     */
    open val approximateDefinitelyNotNullTypes: Boolean get() = false
    open val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.TO_COMMON_SUPERTYPE
    open val approximateIntersectionTypesInContravariantPositions get() = false
    open val approximateLocalTypes get() = false

    /**
     * Defines additional condition for local type approximation.
     * Should return false if the current local type should be considered as a final approximation.
     * This check is triggered for every found local supertype of the initial type (including the type itself).
     * Note that [approximateLocalTypes] should be true for this to have any effect.
     */
    open fun shouldApproximateLocalType(ctx: TypeSystemInferenceExtensionContext, type: KotlinTypeMarker): Boolean =
        true

    /**
     * Is only expected to be true for FinalApproximationAfterResolutionAndInference
     * But it's only used for K2 to reproduce K1 behavior for the approximation of resolved calls
     */
    open val convertToNonRawVersionAfterApproximationInK2 get() = false

    /**
     * Whether to approximate anonymous type. This flag does not have any effect if `localTypes` is true because all anonymous types are
     * local.
     */
    open val approximateAnonymous get() = false

    /**
     * This function determines the approximator behavior if a type variable based type is encountered.
     *
     * @param marker type variable encountered
     * @param isK2 true for K2 compiler, false for K1 compiler
     * @return true if the type variable based type should be kept, false if it should be approximated
     */
    internal open fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker, isK2: Boolean): Boolean = true

    context(ctx: TypeSystemInferenceExtensionContext)
    open fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
        return true  // false means that this type we can leave as is
    }

    object LocalDeclaration : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        // Probably, it's worth thinking of returning true only for delegated property accessors, see KT-61090
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker, isK2: Boolean): Boolean = !isK2
    }

    abstract class PublicDeclaration(
        override val approximateLocalTypes: Boolean,
        override val approximateAnonymous: Boolean,
    ) : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        // Probably, it's worth thinking of returning true only for delegated property accessors, see KT-61090
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker, isK2: Boolean): Boolean = !isK2

        object SaveAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = false)
        object ApproximateAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = true)
        object ApproximateLocalAndAnonymousTypes : PublicDeclaration(approximateLocalTypes = true, approximateAnonymous = true)
    }

    /**
     * This kind of configuration is supposed only to approximate some captured types/ILTs and doesn't approximate flexible/error ones.
     */
    sealed class AbstractCapturedTypesAndILTApproximation(private val approximatedCapturedStatus: CaptureStatus?) :
        TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false

        // i.e. will be approximated only approximatedCapturedStatus captured types
        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            return approximatedCapturedStatus != null && type.captureStatus() == approximatedCapturedStatus
        }

        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker, isK2: Boolean): Boolean = false
    }

    object IncorporationConfiguration : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FOR_INCORPORATION) {
        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            if (super.shouldApproximateCapturedType(type)) return true

            if (!ctx.isK2) return false

            return type.contains { nested -> nested is CapturedTypeMarker && super.shouldApproximateCapturedType(nested) }
        }
    }

    object SubtypeCapturedTypesApproximation : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FOR_SUBTYPING)

    class TopLevelIntegerLiteralTypeApproximationWithExpectedType(
        override val expectedTypeForIntegerLiteralType: KotlinTypeMarker?,
    ) : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
    }

    object InternalTypesApproximation : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    object FinalApproximationAfterResolutionAndInference :
        AbstractCapturedTypesAndILTApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        override val convertToNonRawVersionAfterApproximationInK2: Boolean get() = true
    }

    @K2Only
    object IntermediateApproximationToSupertypeAfterCompletionInK2 :
        AbstractCapturedTypesAndILTApproximation(null) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        override val convertToNonRawVersionAfterApproximationInK2: Boolean get() = true

        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            /**
             * Only approximate captured types when they contain a raw supertype.
             * This is an awful hack required to keep K1 compatibility.
             * See [convertToNonRawVersionAfterApproximationInK2].
             */
            return type.captureStatus() == CaptureStatus.FROM_EXPRESSION && type.hasRawSuperTypeRecursive()
        }
    }

    @K2Only
    object TypeArgumentApproximationAfterCompletionInK2 : AbstractCapturedTypesAndILTApproximation(null) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    @AllowedToUsedOnlyInK1
    object IntegerLiteralsTypesApproximation : TypeApproximatorConfiguration() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateAllFlexible: Boolean get() = false
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker, isK2: Boolean): Boolean = false
        override val approximateErrorTypes: Boolean get() = false

        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean = false
    }

    @AllowedToUsedOnlyInK1
    object UpperBoundAwareIntersectionTypeApproximator : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE
    }

    object FrontendToBackendTypesApproximation : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }
}

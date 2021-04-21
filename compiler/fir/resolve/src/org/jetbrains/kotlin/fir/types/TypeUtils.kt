/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*

fun ConeInferenceContext.commonSuperTypeOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> with(NewCommonSuperTypeCalculator) {
            commonSuperType(types) as ConeKotlinType
        }
    }
}

fun ConeInferenceContext.intersectTypesOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> ConeTypeIntersector.intersectTypes(this, types)
    }
}

fun TypeCheckerProviderContext.equalTypes(a: ConeKotlinType, b: ConeKotlinType): Boolean =
    AbstractTypeChecker.equalTypes(this, a, b)

fun ConeDefinitelyNotNullType.Companion.create(original: ConeKotlinType): ConeDefinitelyNotNullType? {
    return when {
        original is ConeDefinitelyNotNullType -> original
        makesSenseToBeDefinitelyNotNull(original) ->
            ConeDefinitelyNotNullType(original.lowerBoundIfFlexible())
        else -> null
    }
}

fun ConeKotlinType.makeConeTypeDefinitelyNotNullOrNotNull(): ConeKotlinType {
    if (this is ConeIntersectionType) {
        return ConeIntersectionType(intersectedTypes.map { it.makeConeTypeDefinitelyNotNullOrNotNull() })
    }
    return ConeDefinitelyNotNullType.create(this) ?: this.withNullability(ConeNullability.NOT_NULL)
}

fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType.create(original.withArguments(arguments))!! as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}

fun <T : ConeKotlinType> T.withAttributes(attributes: ConeAttributes): T {
    if (this.attributes == attributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable, attributes)
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType.create(original.withAttributes(attributes))!!
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable, attributes)
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        else -> error("Not supported: $this: ${this.render()}")
    } as T
}

fun <T : ConeKotlinType> T.withNullability(
    nullability: ConeNullability,
    typeContext: ConeInferenceContext? = null,
    attributes: ConeAttributes = this.attributes,
): T {
    if (this.nullability == nullability && this.attributes == attributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable, attributes)
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable, attributes)
        is ConeFlexibleType -> {
            if (nullability == ConeNullability.UNKNOWN) {
                if (lowerBound.nullability != upperBound.nullability || lowerBound.nullability == ConeNullability.UNKNOWN) {
                    return this
                }
            }
            coneFlexibleOrSimpleType(typeContext, lowerBound.withNullability(nullability), upperBound.withNullability(nullability))
        }
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag)
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor, attributes)
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability)
            }
            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        }
        is ConeStubType -> ConeStubType(variable, nullability)
        is ConeDefinitelyNotNullType -> when (nullability) {
            ConeNullability.NOT_NULL -> this
            ConeNullability.NULLABLE -> original.withNullability(nullability)
            ConeNullability.UNKNOWN -> original.withNullability(nullability)
        }
        is ConeIntegerLiteralType -> ConeIntegerLiteralTypeImpl(value, isUnsigned, nullability)
        else -> error("sealed: ${this::class}")
    } as T
}

fun coneFlexibleOrSimpleType(
    typeContext: ConeInferenceContext?,
    lowerBound: ConeKotlinType,
    upperBound: ConeKotlinType,
): ConeKotlinType {
    if (lowerBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound.lowerBound, upperBound)
    }
    if (upperBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound, upperBound.upperBound)
    }
    return when {
        typeContext != null && AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound) -> {
            lowerBound
        }
        typeContext == null && lowerBound == upperBound -> {
            lowerBound
        }
        else -> {
            ConeFlexibleType(lowerBound, upperBound)
        }
    }
}

fun ConeKotlinType.isExtensionFunctionType(session: FirSession): Boolean {
    val type = this.lowerBoundIfFlexible().fullyExpandedType(session)
    return type.attributes.extensionFunctionType != null
}

fun FirTypeRef.isExtensionFunctionType(session: FirSession): Boolean {
    return coneTypeSafe<ConeKotlinType>()?.isExtensionFunctionType(session) == true
}

fun ConeKotlinType.isUnsafeVarianceType(session: FirSession): Boolean {
    val type = this.lowerBoundIfFlexible().fullyExpandedType(session)
    return type.attributes.unsafeVarianceType != null
}

fun ConeKotlinType.toSymbol(session: FirSession): AbstractFirBasedSymbol<*>? {
    return (this as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

fun FirTypeRef.isUnsafeVarianceType(session: FirSession): Boolean {
    return coneTypeSafe<ConeKotlinType>()?.isUnsafeVarianceType(session) == true
}

fun FirTypeRef.hasEnhancedNullability(): Boolean =
    coneTypeSafe<ConeKotlinType>()?.hasEnhancedNullability == true

fun FirTypeRef.withoutEnhancedNullability(): FirTypeRef {
    require(this is FirResolvedTypeRef)
    if (!hasEnhancedNullability()) return this
    return buildResolvedTypeRef {
        source = this@withoutEnhancedNullability.source
        type = this@withoutEnhancedNullability.type.withAttributes(
            ConeAttributes.create(
                this@withoutEnhancedNullability.type.attributes.filter { it != CompilerConeAttributes.EnhancedNullability }
            )
        )
        annotations += this@withoutEnhancedNullability.annotations
    }
}

// Unlike other cases, return types may be implicit, i.e. unresolved
// But in that cases newType should also be `null`
fun FirTypeRef.withReplacedReturnType(newType: ConeKotlinType?): FirTypeRef {
    require(this is FirResolvedTypeRef || newType == null)
    if (newType == null) return this

    return resolvedTypeFromPrototype(newType)
}

fun FirTypeRef.withReplacedConeType(
    newType: ConeKotlinType?,
    firFakeSourceElementKind: FirFakeSourceElementKind? = null
): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    val newSource =
        if (firFakeSourceElementKind != null)
            this.source?.fakeElement(firFakeSourceElementKind)
        else
            this.source

    return if (newType is ConeKotlinErrorType) {
        buildErrorTypeRef {
            source = newSource
            diagnostic = newType.diagnostic
        }
    } else {
        buildResolvedTypeRef {
            source = newSource
            type = newType
            annotations += this@withReplacedConeType.annotations
            delegatedTypeRef = this@withReplacedConeType.delegatedTypeRef
        }
    }
}

fun FirTypeRef.approximated(
    typeApproximator: ConeTypeApproximator,
    toSuper: Boolean,
    conf: TypeApproximatorConfiguration = TypeApproximatorConfiguration.PublicDeclaration
): FirTypeRef {
    val approximatedType = if (toSuper)
        typeApproximator.approximateToSuperType(coneType, conf)
    else
        typeApproximator.approximateToSubType(coneType, conf)
    return withReplacedConeType(approximatedType)
}

fun FirTypeRef.approximatedIfNeededOrSelf(
    approximator: ConeTypeApproximator,
    containingCallableVisibility: Visibility?,
    isInlineFunction: Boolean = false
): FirTypeRef {
    val approximated = if (containingCallableVisibility == Visibilities.Public || containingCallableVisibility == Visibilities.Protected)
        approximatedForPublicPosition(approximator)
    else
        this
    return approximated.hideLocalTypeIfNeeded(containingCallableVisibility, isInlineFunction).withoutEnhancedNullability()
}

fun FirTypeRef.approximatedForPublicPosition(approximator: ConeTypeApproximator): FirTypeRef =
    if (this is FirResolvedTypeRef && type.requiresApproximationInPublicPosition())
        this.approximated(approximator, toSuper = true)
    else
        this

private fun ConeKotlinType.requiresApproximationInPublicPosition(): Boolean = contains {
    it is ConeIntegerLiteralType || it is ConeCapturedType || it is ConeDefinitelyNotNullType || it is ConeIntersectionType
}

/*
 * Suppose a function without an explicit return type just returns an anonymous object:
 *
 *   fun foo(...) = object : ObjectSuperType {
 *     override fun ...
 *   }
 *
 * Without unwrapping, the return type ended up with that anonymous object (<no name provided>), while the resolved super type, which
 * acts like an implementing interface, is a better fit. In fact, exposing an anonymous object types is prohibited for certain cases,
 * e.g., KT-33917. We can also apply this to any local types.
 */
private fun FirTypeRef.hideLocalTypeIfNeeded(
    containingCallableVisibility: Visibility?,
    isInlineFunction: Boolean = false
): FirTypeRef {
    if (containingCallableVisibility == null) {
        return this
    }
    // Approximate types for non-private (all but package private or private) members.
    // Also private inline functions, as per KT-33917.
    if (containingCallableVisibility == Visibilities.Public ||
        containingCallableVisibility == Visibilities.Protected ||
        containingCallableVisibility == Visibilities.Internal ||
        (containingCallableVisibility == Visibilities.Private && isInlineFunction)
    ) {
        val firClass =
            (((this as? FirResolvedTypeRef)
                ?.type as? ConeClassLikeType)
                ?.lookupTag as? ConeClassLookupTagWithFixedSymbol)
                ?.symbol?.fir
        if (firClass !is FirAnonymousObject) {
            // NB: local classes are acceptable here, but reported by EXPOSED_* checkers as errors
            return this
        }
        if (firClass.superTypeRefs.size > 1) {
            return buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("Cannot hide local type ${firClass.render()}")
            }
        }
        val superType = firClass.superTypeRefs.single()
        if (superType is FirResolvedTypeRef) {
            return superType
        }
    }
    return this
}

fun ConeTypeContext.captureFromArgumentsInternal(type: ConeKotlinType, status: CaptureStatus): ConeKotlinType? {
    val capturedArguments = captureArguments(type, status) ?: return null
    return if (type is ConeFlexibleType) {
        ConeFlexibleType(
            type.lowerBound.withArguments(capturedArguments),
            type.upperBound.withArguments(capturedArguments),
        )
    } else {
        type.withArguments(capturedArguments)
    }
}

private fun ConeTypeContext.captureArguments(type: ConeKotlinType, status: CaptureStatus): Array<ConeTypeProjection>? {
    val argumentsCount = type.typeArguments.size
    if (argumentsCount == 0) return null

    val typeConstructor = type.typeConstructor()
    if (argumentsCount != typeConstructor.parametersCount()) return null

    if (type.typeArguments.all { it !is ConeStarProjection && it.kind == ProjectionKind.INVARIANT }) return null

    val newArguments = Array(argumentsCount) { index ->
        val argument = type.typeArguments[index]
        if (argument !is ConeStarProjection && argument.kind == ProjectionKind.INVARIANT) return@Array argument

        val lowerType = if (argument !is ConeStarProjection && argument.getVariance() == TypeVariance.IN) {
            (argument as ConeKotlinTypeProjection).type
        } else {
            null
        }

        ConeCapturedType(status, lowerType, argument, typeConstructor.getParameter(index))
    }

    val substitution = (0 until argumentsCount).map { index ->
        (typeConstructor.getParameter(index) as ConeTypeParameterLookupTag).symbol to (newArguments[index] as ConeKotlinType)
    }.toMap()
    val substitutor = substitutorByMap(substitution, session)

    for (index in 0 until argumentsCount) {
        val oldArgument = type.typeArguments[index]
        val newArgument = newArguments[index]

        if (oldArgument !is ConeStarProjection && oldArgument.kind == ProjectionKind.INVARIANT) continue

        val parameter = typeConstructor.getParameter(index)
        val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableListOf()) { paramIndex ->
            substitutor.safeSubstitute(
                this as TypeSystemInferenceExtensionContext, parameter.getUpperBound(paramIndex)
            )
        }

        if (!oldArgument.isStarProjection() && oldArgument.getVariance() == TypeVariance.OUT) {
            upperBounds += oldArgument.getType()
        }

        require(newArgument is ConeCapturedType)
        @Suppress("UNCHECKED_CAST")
        newArgument.constructor.supertypes = upperBounds as List<ConeKotlinType>
    }
    return newArguments
}

fun ConeTypeContext.captureFromExpressionInternal(type: ConeKotlinType): ConeKotlinType? {
    if (type !is ConeIntersectionType && type !is ConeFlexibleType) {
        return captureFromArgumentsInternal(type, CaptureStatus.FROM_EXPRESSION)
    }
    /*
     * We capture arguments in the intersection types in specific way:
     *  1) Firstly, we create captured arguments for all type arguments grouped by a type constructor* and a type argument's type.
     *      It means, that we create only one captured argument for two types `Foo<*>` and `Foo<*>?` within a flexible type, for instance.
     *      * In addition to grouping by type constructors, we look at possibility locating of two types in different bounds of the same flexible type.
     *        This is necessary in order to create the same captured arguments,
     *        for example, for `MutableList` in the lower bound of the flexible type and for `List` in the upper one.
     *        Example: MutableList<*>..List<*>? -> MutableList<Captured1(*)>..List<Captured2(*)>?, Captured1(*) and Captured2(*) are the same.
     *  2) Secondly, we replace type arguments with captured arguments by given a type constructor and type arguments.
     */
    val capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null

    // We reuse `TypeToCapture` for some types, suitability to reuse defines by `isSuitableForType`
    fun findCorrespondingCapturedArgumentsForType(type: ConeKotlinType) =
        capturedArgumentsByComponents.find { typeToCapture -> typeToCapture.isSuitableForType(type, this) }?.capturedArguments

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: ConeKotlinType): List<ConeKotlinType> {
        return if (typeToReplace is ConeIntersectionType) {
            typeToReplace.intersectedTypes.map { componentType ->
                val capturedArguments = findCorrespondingCapturedArgumentsForType(componentType)
                    ?: return@map componentType
                componentType.withArguments(capturedArguments)
            }
        } else {
            val capturedArguments = findCorrespondingCapturedArgumentsForType(typeToReplace)
                ?: return listOf(typeToReplace)
            listOf(typeToReplace.withArguments(capturedArguments))
        }
    }

    return if (type is ConeFlexibleType) {
        val lowerIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound))
            .withNullability(type.lowerBound.isMarkedNullable) as ConeKotlinType
        val upperIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound))
            .withNullability(type.upperBound.isMarkedNullable) as ConeKotlinType

        ConeFlexibleType(lowerIntersectedType, upperIntersectedType)
    } else {
        intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type)).withNullability(type.isMarkedNullable) as ConeKotlinType
    }
}

private fun ConeTypeContext.captureArgumentsForIntersectionType(type: ConeKotlinType): List<CapturedArguments>? {
    // It's possible to have one of the bounds as non-intersection type
    fun getTypesToCapture(type: ConeKotlinType) =
        if (type is ConeIntersectionType) type.intersectedTypes else listOf(type)

    val filteredTypesToCapture =
        when (type) {
            is ConeFlexibleType -> {
                val typesToCapture = getTypesToCapture(type.lowerBound) + getTypesToCapture(type.upperBound)
                typesToCapture.distinctBy {
                    (ConeFlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(it) ?: it.typeConstructor(this)) to it.typeArguments
                }
            }
            is ConeIntersectionType -> type.intersectedTypes
            else -> error("Should not be here")
        }

    var changed = false

    val capturedArgumentsByTypes = filteredTypesToCapture.mapNotNull { typeToCapture ->
        val capturedArguments = captureArguments(typeToCapture, CaptureStatus.FROM_EXPRESSION)
            ?: return@mapNotNull null
        changed = true
        CapturedArguments(capturedArguments, originalType = typeToCapture)
    }

    if (!changed) return null

    return capturedArgumentsByTypes
}

private class CapturedArguments(val capturedArguments: Array<ConeTypeProjection>, private val originalType: ConeKotlinType) {
    fun isSuitableForType(type: ConeKotlinType, context: ConeTypeContext): Boolean {
        val areArgumentsMatched = type.typeArguments.withIndex().all { (i, typeArgumentsType) ->
            originalType.typeArguments.size > i && typeArgumentsType == originalType.typeArguments[i]
        }

        if (!areArgumentsMatched) return false

        val areConstructorsMatched = originalType.typeConstructor(context) == type.typeConstructor(context)
                || ConeFlexibleTypeBoundsChecker.areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(originalType, type)

        if (!areConstructorsMatched) return false

        return true
    }
}


/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copyWithNewSourceKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible as coneLowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible as coneUpperBoundIfFlexible

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

private fun ConeTypeContext.makesSenseToBeDefinitelyNotNull(originalType: ConeKotlinType): Boolean {
    return when (val type = originalType.lowerBoundIfFlexible()) {
        is ConeTypeParameterType -> type.isNullableType()
        // Actually, this branch should work for type parameters as well, but it breaks some cases. See KT-40114.
        // Basically, if we have `T : X..X?`, then `T <: Any` but we still have `T` != `T & Any`.
        is ConeTypeVariableType, is ConeCapturedType -> {
            !AbstractNullabilityChecker.isSubtypeOfAny(
                newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false), type
            )
        }
        // For all other types `T & Any` is the same as `T` without a question mark.
        else -> false
    }
}

fun ConeDefinitelyNotNullType.Companion.create(
    original: ConeKotlinType,
    typeContext: ConeTypeContext
): ConeDefinitelyNotNullType? {
    return when (original) {
        is ConeDefinitelyNotNullType -> original
        is ConeFlexibleType -> create(original.lowerBound, typeContext)
        is ConeSimpleKotlinType -> runIf(typeContext.makesSenseToBeDefinitelyNotNull(original)) {
            ConeDefinitelyNotNullType(original.coneLowerBoundIfFlexible())
        }
    }
}

fun ConeKotlinType.makeConeTypeDefinitelyNotNullOrNotNull(typeContext: ConeTypeContext): ConeKotlinType {
    if (this is ConeIntersectionType) {
        return ConeIntersectionType(intersectedTypes.map { it.makeConeTypeDefinitelyNotNullOrNotNull(typeContext) })
    }
    return ConeDefinitelyNotNullType.create(this, typeContext) ?: this.withNullability(ConeNullability.NOT_NULL, typeContext)
}

fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(original.withArguments(arguments)) as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}

fun <T : ConeKotlinType> T.withArguments(replacement: (ConeTypeProjection) -> ConeTypeProjection) =
    withArguments(typeArguments.map(replacement).toTypedArray())

fun <T : ConeKotlinType> T.withAttributes(attributes: ConeAttributes): T {
    if (this.attributes == attributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable, attributes)
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(original.withAttributes(attributes))
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable, attributes)
        is ConeRawType -> ConeRawType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag, attributes)
        is ConeCapturedType -> ConeCapturedType(
            captureStatus, lowerType, nullability, constructor, attributes, isProjectionNotNull,
        )
        // TODO: Consider correct application of attributes to ConeIntersectionType
        // Currently, ConeAttributes.union works a bit strange, because it lefts only `other` parts
        is ConeIntersectionType -> this
        // Attributes for stub types are not supported, and it's not obvious if it should
        is ConeStubType -> this
        is ConeIntegerLiteralType -> this
        else -> error("Not supported: $this: ${this.render()}")
    } as T
}

fun <T : ConeKotlinType> T.withNullability(
    nullability: ConeNullability,
    typeContext: ConeTypeContext,
    attributes: ConeAttributes = this.attributes,
): T {
    if (this.nullability == nullability && this.attributes == attributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable, attributes)
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable, attributes)
        is ConeFlexibleType -> {
            if (nullability == ConeNullability.UNKNOWN) {
                if (lowerBound.nullability != upperBound.nullability || lowerBound.nullability == ConeNullability.UNKNOWN) {
                    return this
                }
            }
            coneFlexibleOrSimpleType(
                typeContext,
                lowerBound.withNullability(nullability, typeContext),
                upperBound.withNullability(nullability, typeContext)
            )
        }
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag)
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor, attributes)
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability, typeContext)
            }
            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        }
        is ConeStubTypeForSyntheticFixation -> ConeStubTypeForSyntheticFixation(constructor, nullability)
        is ConeStubTypeForChainInference -> ConeStubTypeForChainInference(constructor, nullability)
        is ConeStubTypeForTypeVariableInSubtyping -> ConeStubTypeForTypeVariableInSubtyping(constructor, nullability)
        is ConeDefinitelyNotNullType -> when (nullability) {
            ConeNullability.NOT_NULL -> this
            ConeNullability.NULLABLE -> original.withNullability(nullability, typeContext)
            ConeNullability.UNKNOWN -> original.withNullability(nullability, typeContext)
        }
        is ConeIntegerLiteralConstantType -> ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, nullability)
        is ConeIntegerConstantOperatorType -> ConeIntegerConstantOperatorTypeImpl(isUnsigned, nullability)
        else -> error("sealed: ${this::class}")
    } as T
}

fun coneFlexibleOrSimpleType(
    typeContext: ConeTypeContext,
    lowerBound: ConeKotlinType,
    upperBound: ConeKotlinType,
): ConeKotlinType {
    return when (lowerBound) {
        is ConeFlexibleType -> coneFlexibleOrSimpleType(typeContext, lowerBound.lowerBound, upperBound)
        is ConeSimpleKotlinType -> when (upperBound) {
            is ConeFlexibleType -> coneFlexibleOrSimpleType(typeContext, lowerBound, upperBound.upperBound)
            is ConeSimpleKotlinType -> when {
                AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound) -> lowerBound
                else -> ConeFlexibleType(lowerBound, upperBound)
            }
        }
    }
}

fun ConeKotlinType.isExtensionFunctionType(session: FirSession): Boolean {
    val type = this.coneLowerBoundIfFlexible().fullyExpandedType(session)
    return type.attributes.extensionFunctionType != null
}

fun FirTypeRef.isExtensionFunctionType(session: FirSession): Boolean {
    return coneTypeSafe<ConeKotlinType>()?.isExtensionFunctionType(session) == true
}

fun ConeKotlinType.isUnsafeVarianceType(session: FirSession): Boolean {
    val type = this.coneLowerBoundIfFlexible().fullyExpandedType(session)
    return type.attributes.unsafeVarianceType != null
}

fun ConeKotlinType.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return (this as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

fun ConeClassLikeType.toSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

fun ConeKotlinType.toFirResolvedTypeRef(
    source: KtSourceElement? = null,
    delegatedTypeRef: FirTypeRef? = null
): FirResolvedTypeRef {
    return if (this is ConeErrorType) {
        buildErrorTypeRef {
            this.source = source
            diagnostic = this@toFirResolvedTypeRef.diagnostic
            type = this@toFirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
    } else {
        buildResolvedTypeRef {
            this.source = source
            type = this@toFirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
    }
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
            ),
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
    firFakeSourceElementKind: KtFakeSourceElementKind? = null
): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    val newSource =
        if (firFakeSourceElementKind != null)
            this.source?.fakeElement(firFakeSourceElementKind)
        else
            this.source

    return if (newType is ConeErrorType) {
        buildErrorTypeRef {
            source = newSource
            type = newType
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
): FirTypeRef {
    val alternativeType = (coneType as? ConeIntersectionType)?.alternativeType ?: coneType
    if (alternativeType !== coneType && !alternativeType.requiresApproximationInPublicPosition()) {
        return withReplacedConeType(alternativeType)
    }
    val approximatedType = if (toSuper)
        typeApproximator.approximateToSuperType(alternativeType, TypeApproximatorConfiguration.PublicDeclaration)
    else
        typeApproximator.approximateToSubType(alternativeType, TypeApproximatorConfiguration.PublicDeclaration)
    return withReplacedConeType(approximatedType)
}

fun FirTypeRef.approximatedIfNeededOrSelf(
    approximator: ConeTypeApproximator,
    containingCallableVisibility: Visibility?,
    isInlineFunction: Boolean = false,
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
    if (!shouldHideLocalType(containingCallableVisibility, isInlineFunction)) return this
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
        // NB: don't approximate so members can be resolved. The error is reported by FirAmbiguousAnonymousTypeChecker.
        return this
    }
    val superType = firClass.superTypeRefs.single()
    if (superType is FirResolvedTypeRef) {
        val newKind = source?.kind
        return if (newKind is KtFakeSourceElementKind) superType.copyWithNewSourceKind(newKind) else superType
    }
    return this
}

fun shouldHideLocalType(containingCallableVisibility: Visibility?, isInlineFunction: Boolean): Boolean {
    if (containingCallableVisibility == null) {
        return false
    }
    // Approximate types for non-private (all but package private or private) members.
    // Also private inline functions, as per KT-33917.
    return containingCallableVisibility == Visibilities.Public ||
            containingCallableVisibility == Visibilities.Protected ||
            containingCallableVisibility == Visibilities.Internal ||
            (containingCallableVisibility == Visibilities.Private && isInlineFunction)
}

fun FirDeclaration.visibilityForApproximation(container: FirDeclaration?): Visibility {
    if (this !is FirMemberDeclaration) return Visibilities.Local
    val containerVisibility =
        if (container == null || container is FirFile) Visibilities.Public
        else (container as? FirRegularClass)?.visibility ?: Visibilities.Local
    if (containerVisibility == Visibilities.Local || visibility == Visibilities.Local) return Visibilities.Local
    if (containerVisibility == Visibilities.Private) return Visibilities.Private
    return visibility
}


internal fun ConeTypeContext.captureFromArgumentsInternal(type: ConeKotlinType, status: CaptureStatus): ConeKotlinType? {
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

fun ConeTypeContext.captureArguments(type: ConeKotlinType, status: CaptureStatus): Array<ConeKotlinType>? {
    val argumentsCount = type.typeArguments.size
    if (argumentsCount == 0) return null

    val typeConstructor = type.typeConstructor()
    if (argumentsCount != typeConstructor.parametersCount()) return null

    if (type.typeArguments.all { it.kind == ProjectionKind.INVARIANT }) return null

    val newArguments: Array<ConeKotlinType> = Array(argumentsCount) { index ->
        val argument = type.typeArguments[index]
        if (argument.kind == ProjectionKind.INVARIANT) return@Array argument.type!!

        val lowerType = if (argument.kind == ProjectionKind.IN) {
            (argument as ConeKotlinTypeProjection).type
        } else {
            null
        }

        ConeCapturedType(status, lowerType, argument, typeConstructor.getParameter(index))
    }

    val substitution = (0 until argumentsCount).associate { index ->
        (typeConstructor.getParameter(index) as ConeTypeParameterLookupTag).symbol to (newArguments[index])
    }
    val substitutor = substitutorByMap(substitution, session)

    for (index in 0 until argumentsCount) {
        val oldArgument = type.typeArguments[index]
        val newArgument = newArguments[index]

        if (oldArgument.kind == ProjectionKind.INVARIANT) continue

        val parameter = typeConstructor.getParameter(index)
        val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableListOf()) { paramIndex ->
            substitutor.safeSubstitute(
                this as TypeSystemInferenceExtensionContext, parameter.getUpperBound(paramIndex)
            )
        }

        if (oldArgument.kind == ProjectionKind.OUT) {
            upperBounds += oldArgument.getType()
        }

        require(newArgument is ConeCapturedType)
        @Suppress("UNCHECKED_CAST")
        newArgument.constructor.supertypes = upperBounds as List<ConeKotlinType>
    }
    return newArguments
}

internal fun ConeTypeContext.captureFromExpressionInternal(type: ConeKotlinType): ConeKotlinType? {
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

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: ConeSimpleKotlinType): List<ConeKotlinType> {
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

    return when (type) {
        is ConeFlexibleType -> {
            val lowerIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound))
                .withNullability(ConeNullability.create(type.lowerBound.isMarkedNullable), this)
            val upperIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound))
                .withNullability(ConeNullability.create(type.upperBound.isMarkedNullable), this)

            ConeFlexibleType(lowerIntersectedType.coneLowerBoundIfFlexible(), upperIntersectedType.coneUpperBoundIfFlexible())
        }
        is ConeSimpleKotlinType -> {
            intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type)).withNullability(type.isMarkedNullable) as ConeKotlinType
        }
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

private class CapturedArguments(val capturedArguments: Array<out ConeTypeProjection>, private val originalType: ConeKotlinType) {
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

fun ConeKotlinType.isSubtypeOf(superType: ConeKotlinType, session: FirSession): Boolean =
    AbstractTypeChecker.isSubtypeOf(
        session.typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
        this, superType,
    )

fun FirCallableDeclaration.isSubtypeOf(
    other: FirCallableDeclaration,
    typeCheckerContext: TypeCheckerState
): Boolean {
    return AbstractTypeChecker.isSubtypeOf(
        typeCheckerContext,
        returnTypeRef.coneType,
        other.returnTypeRef.coneType
    )
}

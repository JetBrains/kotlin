/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeRecursiveTypeParameterDuringErasureError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitute
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
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

private fun ConeTypeContext.makesSenseToBeDefinitelyNotNull(
    type: ConeSimpleKotlinType,
    avoidComprehensiveCheck: Boolean,
): Boolean {
    return when (type) {
        is ConeTypeParameterType -> avoidComprehensiveCheck || type.isNullableType()
        // Actually, this branch should work for type parameters as well, but it breaks some cases. See KT-40114.
        // Basically, if we have `T : X..X?`, then `T <: Any` but we still have `T` != `T & Any`.
        is ConeTypeVariableType, is ConeCapturedType -> {
            avoidComprehensiveCheck || !AbstractNullabilityChecker.isSubtypeOfAny(
                newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false), type
            )
        }
        // For all other types `T & Any` is the same as `T` without a question mark.
        else -> false
    }
}

fun ConeDefinitelyNotNullType.Companion.create(
    original: ConeKotlinType,
    typeContext: ConeTypeContext,
    // Sometimes, it might be called before type parameter bounds are initialized
    // or even before the symbols are bound to FIR
    // In such cases, we just assume it makes sense to create DNN there
    // NB: `makesSenseToBeDefinitelyNotNull` is mostly an optimization, it should not affect semantics
    avoidComprehensiveCheck: Boolean = false,
): ConeDefinitelyNotNullType? {
    return when (original) {
        is ConeDefinitelyNotNullType -> original
        is ConeFlexibleType -> create(original.lowerBound, typeContext, avoidComprehensiveCheck)
        is ConeSimpleKotlinType -> runIf(typeContext.makesSenseToBeDefinitelyNotNull(original, avoidComprehensiveCheck)) {
            ConeDefinitelyNotNullType(original.coneLowerBoundIfFlexible())
        }
    }
}

@OptIn(DynamicTypeConstructor::class)
fun ConeDynamicType.Companion.create(
    session: FirSession,
    attributes: ConeAttributes = ConeAttributes.Empty,
) = ConeDynamicType(
    session.builtinTypes.nothingType.type.withAttributes(attributes),
    session.builtinTypes.nullableAnyType.type.withAttributes(attributes),
)

fun ConeKotlinType.makeConeTypeDefinitelyNotNullOrNotNull(
    typeContext: ConeTypeContext,
    avoidComprehensiveCheck: Boolean = false,
): ConeKotlinType {
    if (this is ConeIntersectionType) {
        return ConeIntersectionType(intersectedTypes.map {
            it.makeConeTypeDefinitelyNotNullOrNotNull(typeContext, avoidComprehensiveCheck)
        })
    }
    return ConeDefinitelyNotNullType.create(this, typeContext, avoidComprehensiveCheck)
        ?: this.withNullability(ConeNullability.NOT_NULL, typeContext)
}

fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> ConeErrorType(diagnostic, isUninferredParameter, typeArguments = arguments, attributes = attributes) as T
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, arguments, nullability.isNullable, attributes) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(original.withArguments(arguments)) as T
        else -> errorWithAttachment("Not supported: ${this::class}") {
            withConeTypeEntry("type", this@withArguments)
        }
    }
}

fun <T : ConeKotlinType> T.withArguments(replacement: (ConeTypeProjection) -> ConeTypeProjection) =
    withArguments(typeArguments.map(replacement).toTypedArray())

@OptIn(DynamicTypeConstructor::class)
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
        is ConeRawType -> ConeRawType.create(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeDynamicType -> ConeDynamicType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, typeConstructor, attributes)
        is ConeCapturedType -> ConeCapturedType(
            captureStatus, lowerType, nullability, constructor, attributes, isProjectionNotNull,
        )
        // TODO: Consider correct application of attributes to ConeIntersectionType
        // Currently, ConeAttributes.union works a bit strange, because it lefts only `other` parts
        is ConeIntersectionType -> this
        // Attributes for stub types are not supported, and it's not obvious if it should
        is ConeStubType -> this
        is ConeIntegerLiteralType -> this
        else -> errorWithAttachment("Not supported: ${this::class}") {
            withConeTypeEntry("type", this@withAttributes)
        }
    } as T
}

fun <T : ConeKotlinType> T.withNullability(
    nullability: ConeNullability,
    typeContext: ConeTypeContext,
    attributes: ConeAttributes = this.attributes,
    preserveEnhancedNullability: Boolean = false,
): T {
    val theAttributes = attributes.butIf(!preserveEnhancedNullability) {
        val withoutEnhanced = it.remove(CompilerConeAttributes.EnhancedNullability)
        withoutEnhanced.transformTypesWith { t -> t.withNullability(nullability, typeContext) } ?: withoutEnhanced
    }

    if (this.nullability == nullability && this.attributes == theAttributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable, theAttributes)
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable, theAttributes)
        is ConeDynamicType -> this
        is ConeFlexibleType -> {
            if (nullability == ConeNullability.UNKNOWN) {
                if (lowerBound.nullability != upperBound.nullability || lowerBound.nullability == ConeNullability.UNKNOWN) {
                    return this
                }
            }
            coneFlexibleOrSimpleType(
                typeContext,
                lowerBound.withNullability(nullability, typeContext, preserveEnhancedNullability = preserveEnhancedNullability),
                upperBound.withNullability(nullability, typeContext, preserveEnhancedNullability = preserveEnhancedNullability)
            )
        }

        is ConeTypeVariableType -> ConeTypeVariableType(nullability, typeConstructor, theAttributes)
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor, theAttributes)
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability, typeContext, preserveEnhancedNullability = preserveEnhancedNullability)
            }

            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        }

        is ConeStubTypeForChainInference -> ConeStubTypeForChainInference(constructor, nullability)
        is ConeStubTypeForTypeVariableInSubtyping -> ConeStubTypeForTypeVariableInSubtyping(constructor, nullability)
        is ConeDefinitelyNotNullType -> when (nullability) {
            ConeNullability.NOT_NULL -> this
            ConeNullability.NULLABLE -> original.withNullability(
                nullability, typeContext, preserveEnhancedNullability = preserveEnhancedNullability,
            )
            ConeNullability.UNKNOWN -> original.withNullability(
                nullability, typeContext, preserveEnhancedNullability = preserveEnhancedNullability,
            )
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
            annotations += this@withReplacedConeType.annotations
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

fun shouldApproximateAnonymousTypesOfNonLocalDeclaration(containingCallableVisibility: Visibility?, isInlineFunction: Boolean): Boolean {
    // Approximate types for non-private (all but package private or private) members.
    // Also private inline functions, as per KT-33917.
    return when (containingCallableVisibility) {
        Visibilities.Public,
        Visibilities.Protected,
        Visibilities.Internal -> true
        Visibilities.Private -> isInlineFunction
        else -> false
    }
}

fun FirDeclaration.visibilityForApproximation(container: FirDeclaration?): Visibility {
    if (this !is FirMemberDeclaration) return Visibilities.Local
    val containerVisibility =
        if (container == null || container is FirFile || container is FirScript) Visibilities.Public
        else (container as? FirRegularClass)?.visibility ?: Visibilities.Local
    if (containerVisibility == Visibilities.Local || visibility == Visibilities.Local) return Visibilities.Local
    if (containerVisibility == Visibilities.Private) return Visibilities.Private
    return visibility
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
        (parameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)
        val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableListOf()) { paramIndex ->
            substitutor.safeSubstitute(
                this as TypeSystemInferenceExtensionContext, parameter.getUpperBound(paramIndex)
            )
        }

        if (oldArgument.kind == ProjectionKind.OUT) {
            upperBounds += oldArgument.getType()
        }

        require(newArgument is ConeCapturedType)

        newArgument.constructor.supertypes = if (status == CaptureStatus.FROM_EXPRESSION) {
            // By intersecting the bounds and the projection type, we eliminate "redundant" super types.
            // Redundant is defined by the implementation of the type intersector,
            // e.g., at the moment of writing the intersection of Foo<String!> and Foo<String> was Foo<String>.
            // Note, it's not just an optimization, but actually influences behavior because the nullability can change like in the
            // example above.
            // We only run it for status == CaptureStatus.FROM_EXPRESSION to prevent infinite loops with recursive types because
            // during intersection AbstractTypeChecker is called which in turn can capture super types with status
            // CaptureStatus.FOR_SUBTYPING.
            val intersectedUpperBounds = intersectTypes(upperBounds)
            if (intersectedUpperBounds is ConeIntersectionType) {
                intersectedUpperBounds.intersectedTypes.toList()
            } else {
                listOf(intersectedUpperBounds)
            }
        } else {
            @Suppress("UNCHECKED_CAST")
            upperBounds as List<ConeKotlinType>
        }
    }
    return newArguments
}

internal fun ConeTypeContext.captureFromExpressionInternal(type: ConeKotlinType): ConeKotlinType? {
    if (type is ConeCapturedType) {
        return type.substitute { captureFromExpressionInternal(it) }
    }

    if (type is ConeDefinitelyNotNullType) {
        return captureFromExpressionInternal(type.original)?.makeConeTypeDefinitelyNotNullOrNotNull(this)
    }

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

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: ConeSimpleKotlinType): List<ConeKotlinType>? {
        return if (typeToReplace is ConeIntersectionType) {
            typeToReplace.intersectedTypes.map { componentType ->
                val capturedArguments = findCorrespondingCapturedArgumentsForType(componentType)
                    ?: return@map componentType
                componentType.withArguments(capturedArguments)
            }.takeUnless { it == typeToReplace.intersectedTypes }
        } else {
            val capturedArguments = findCorrespondingCapturedArgumentsForType(typeToReplace)
                ?: return null
            listOf(typeToReplace.withArguments(capturedArguments))
        }
    }

    return when (type) {
        is ConeFlexibleType -> {
            // Flexible types can either have projections in both bounds or just the upper bound (raw types and arrays).
            // Since the scope of flexible types is built from the lower bound, we don't gain any safety from only capturing the
            // upper bound.
            // At the same time, capturing of raw(-like) types leads to issues like KT-63982 or breaks tests like
            // testData/codegen/box/reflection/typeOf/rawTypes_after.kt.
            // Therefore, we return null if nothing was captured for either bound.

            val lowerIntersectedType =
                intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound) ?: return null)
                    .withNullability(ConeNullability.create(type.lowerBound.isMarkedNullable), this)
            val upperIntersectedType =
                intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound) ?: return null)
                    .withNullability(ConeNullability.create(type.upperBound.isMarkedNullable), this)

            ConeFlexibleType(lowerIntersectedType.coneLowerBoundIfFlexible(), upperIntersectedType.coneUpperBoundIfFlexible())
        }

        is ConeSimpleKotlinType -> {
            intersectTypes(
                replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type) ?: return null
            ).withNullability(type.isMarkedNullable) as ConeKotlinType
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

fun ConeKotlinType.isSubtypeOf(superType: ConeKotlinType, session: FirSession, errorTypesEqualToAnything: Boolean = false): Boolean =
    AbstractTypeChecker.isSubtypeOf(
        session.typeContext.newTypeCheckerState(errorTypesEqualToAnything, stubTypesEqualToAnything = false),
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

fun ConeKotlinType.canHaveSubtypes(session: FirSession): Boolean {
    if (this.isMarkedNullable) {
        return true
    }
    val expandedType = fullyExpandedType(session)
    val classSymbol = expandedType.toSymbol(session) as? FirRegularClassSymbol ?: return true
    if (classSymbol.isEnumClass || classSymbol.isExpect || classSymbol.modality != Modality.FINAL) {
        return true
    }

    classSymbol.typeParameterSymbols.forEachIndexed { idx, typeParameterSymbol ->
        val typeProjection = expandedType.typeArguments[idx]

        if (typeProjection.isStarProjection) {
            return true
        }

        val argument = typeProjection.type!! //safe because it is not a star

        when (typeParameterSymbol.variance) {
            Variance.INVARIANT ->
                when (typeProjection.kind) {
                    ProjectionKind.INVARIANT ->
                        if (lowerThanBound(session.typeContext, argument, typeParameterSymbol) || argument.canHaveSubtypes(session)) {
                            return true
                        }

                    ProjectionKind.IN ->
                        if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                            return true
                        }

                    ProjectionKind.OUT ->
                        if (argument.canHaveSubtypes(session)) {
                            return true
                        }

                    ProjectionKind.STAR ->
                        return true
                }

            Variance.IN_VARIANCE ->
                if (typeProjection.kind != ProjectionKind.OUT) {
                    if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                        return true
                    }
                } else {
                    if (argument.canHaveSubtypes(session)) {
                        return true
                    }
                }

            Variance.OUT_VARIANCE ->
                if (typeProjection.kind != ProjectionKind.IN) {
                    if (argument.canHaveSubtypes(session)) {
                        return true
                    }
                } else {
                    if (lowerThanBound(session.typeContext, argument, typeParameterSymbol)) {
                        return true
                    }
                }
        }
    }

    return false
}

/**
 * Returns the FirRegularClassSymbol associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
}

fun ConeKotlinType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return (this as? ConeClassLikeType)?.toRegularClassSymbol(session)
}

fun ConeClassLikeType.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return fullyExpandedType(session).toSymbol(session) as? FirClassSymbol<*>
}

private fun lowerThanBound(context: ConeInferenceContext, argument: ConeKotlinType, typeParameterSymbol: FirTypeParameterSymbol): Boolean {
    typeParameterSymbol.resolvedBounds.forEach { boundTypeRef ->
        if (argument != boundTypeRef.coneType && argument.isSubtypeOf(context, boundTypeRef.coneType)) {
            return true
        }
    }
    return false
}

fun KotlinTypeMarker.isSubtypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, this, type)

fun List<FirTypeParameterSymbol>.eraseToUpperBoundsAssociated(
    session: FirSession,
): Map<FirTypeParameterSymbol, ConeKotlinType> {
    val cache = mutableMapOf<FirTypeParameter, ConeKotlinType>()
    return associateWith {
        it.fir.eraseToUpperBound(session, cache, mode = EraseUpperBoundMode.FOR_EMPTY_INTERSECTION_CHECK)
    }
}

fun List<FirTypeParameterSymbol>.getProjectionsForRawType(session: FirSession): Array<ConeTypeProjection> {
    val cache = mutableMapOf<FirTypeParameter, ConeKotlinType>()
    return Array(size) { index ->
        this[index].fir.eraseToUpperBound(
            session, cache, mode = EraseUpperBoundMode.FOR_RAW_TYPE_ERASURE
        )
    }
}

private enum class EraseUpperBoundMode {
    FOR_RAW_TYPE_ERASURE,
    FOR_EMPTY_INTERSECTION_CHECK
}

private fun FirTypeParameter.eraseToUpperBound(
    session: FirSession,
    cache: MutableMap<FirTypeParameter, ConeKotlinType>,
    mode: EraseUpperBoundMode,
): ConeKotlinType {
    fun eraseAsUpperBound(type: FirResolvedTypeRef) =
        type.coneType.eraseAsUpperBound(session, cache, mode)

    return cache.getOrPut(this) {
        // Mark to avoid loops.
        cache[this] = ConeErrorType(ConeRecursiveTypeParameterDuringErasureError(name))
        if (mode == EraseUpperBoundMode.FOR_EMPTY_INTERSECTION_CHECK) {
            ConeTypeIntersector.intersectTypes(session.typeContext, symbol.resolvedBounds.map(::eraseAsUpperBound))
        } else {
            when (val boundTypeRef = bounds.first()) {
                is FirResolvedTypeRef -> eraseAsUpperBound(boundTypeRef)
                // While resolving raw supertype in Java we may encounter a situation
                // when this supertype constructor has some type parameters and
                // their bounds aren't yet resolved. See KT-56630 and comments inside.
                // Yet we are replacing these bounds with just 'Any'.
                // TODO: think how can we replace it with more correct decision.
                else -> session.builtinTypes.anyType.type
            }
        }
    }
}

private fun SimpleTypeMarker.eraseArgumentsDeeply(
    typeContext: ConeInferenceContext,
    cache: MutableMap<FirTypeParameter, ConeKotlinType>,
    mode: EraseUpperBoundMode,
): ConeKotlinType = with(typeContext) {
    replaceArgumentsDeeply { typeArgument ->
        if (typeArgument.isStarProjection())
            return@replaceArgumentsDeeply typeArgument

        val typeConstructor = typeArgument.getType().typeConstructor().takeIf { it.isTypeParameterTypeConstructor() }
            ?: return@replaceArgumentsDeeply typeArgument

        typeConstructor as ConeTypeParameterLookupTag

        val erasedType = typeConstructor.typeParameterSymbol.fir.eraseToUpperBound(
            session, cache, mode = mode
        )

        if ((erasedType as? ConeErrorType)?.diagnostic is ConeRecursiveTypeParameterDuringErasureError)
            return@replaceArgumentsDeeply ConeStarProjection

        // See the similar semantics at RawProjectionComputer::computeProjection
        if (mode == EraseUpperBoundMode.FOR_RAW_TYPE_ERASURE)
            erasedType
        else
            erasedType.toTypeProjection(ProjectionKind.OUT)
    } as ConeKotlinType
}

private fun ConeKotlinType.eraseAsUpperBound(
    session: FirSession,
    cache: MutableMap<FirTypeParameter, ConeKotlinType>,
    mode: EraseUpperBoundMode,
): ConeKotlinType =
    when (this) {
        is ConeClassLikeType -> {
            eraseArgumentsDeeply(session.typeContext, cache, mode)
        }

        is ConeFlexibleType ->
            // If one bound is a type parameter, the other is probably the same type parameter,
            // so there is no exponential complexity here due to cache lookups.
            coneFlexibleOrSimpleType(
                session.typeContext,
                lowerBound.eraseAsUpperBound(session, cache, mode),
                upperBound.eraseAsUpperBound(session, cache, mode)
            )

        is ConeTypeParameterType ->
            lookupTag.typeParameterSymbol.fir.eraseToUpperBound(
                session, cache, mode
            ).let {
                if (isNullable) it.withNullability(nullability, session.typeContext) else it
            }

        is ConeDefinitelyNotNullType ->
            original.eraseAsUpperBound(session, cache, mode)
                .makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)

        else -> errorWithAttachment("unexpected Java type parameter upper bound kind: ${this::class}") {
            withConeTypeEntry("type", this@eraseAsUpperBound)
        }
    }

fun ConeKotlinType.isRaw(): Boolean = lowerBoundIfFlexible().attributes.contains(CompilerConeAttributes.RawType)

fun ConeKotlinType.convertToNonRawVersion(): ConeKotlinType {
    if (!isRaw()) return this

    if (this is ConeFlexibleType) {
        return ConeFlexibleType(
            lowerBound.withAttributes(this.attributes.remove(CompilerConeAttributes.RawType)),
            upperBound,
        )
    }

    return withAttributes(attributes.remove(CompilerConeAttributes.RawType))
}

fun ConeKotlinType.canBeNull(session: FirSession): Boolean {
    if (isMarkedNullable) {
        return true
    }
    return when (this) {
        is ConeFlexibleType -> upperBound.canBeNull(session)
        is ConeDefinitelyNotNullType -> false
        is ConeTypeParameterType -> this.lookupTag.typeParameterSymbol.resolvedBounds.all {
            // Upper bounds can resolve to typealiases that can expand to nullable types.
            it.coneType.fullyExpandedType(session).canBeNull(session)
        }
        is ConeIntersectionType -> intersectedTypes.all { it.canBeNull(session) }
        is ConeCapturedType -> constructor.supertypes?.all { it.canBeNull(session) } == true
        else -> fullyExpandedType(session).isNullable
    }
}

fun FirIntersectionTypeRef.isLeftValidForDefinitelyNotNullable(session: FirSession) =
    leftType.coneType.let { it is ConeTypeParameterType && it.canBeNull(session) && !it.isMarkedNullable }

val FirIntersectionTypeRef.isRightValidForDefinitelyNotNullable get() = rightType.coneType.isAny
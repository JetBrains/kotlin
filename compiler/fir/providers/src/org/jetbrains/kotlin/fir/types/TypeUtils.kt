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
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithNullability
import org.jetbrains.kotlin.fir.diagnostics.ConeRecursiveTypeParameterDuringErasureError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.wrapProjection
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
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
            ConeDefinitelyNotNullType(original)
        }
    }
}

@OptIn(DynamicTypeConstructor::class)
fun ConeDynamicType.Companion.create(
    session: FirSession,
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeDynamicType = ConeDynamicType(
    session.builtinTypes.nothingType.coneType.withAttributes(attributes),
    session.builtinTypes.nullableAnyType.coneType.withAttributes(attributes),
)

fun ConeKotlinType.makeConeTypeDefinitelyNotNullOrNotNull(
    typeContext: ConeTypeContext,
    avoidComprehensiveCheck: Boolean = false,
    preserveAttributes: Boolean = false,
): ConeKotlinType {
    // It's necessary to properly handling the situation like `typealias Foo = Any?`
    // NB: It's not related to actual type aliases since they can't refer nullable types
    fullyExpandedType(typeContext.session).let { expandedType ->
        if (expandedType !== this) {
            return expandedType.makeConeTypeDefinitelyNotNullOrNotNull(
                typeContext,
                avoidComprehensiveCheck,
                preserveAttributes
            )
        }
    }

    if (this is ConeIntersectionType) {
        return ConeIntersectionType(intersectedTypes.map {
            it.makeConeTypeDefinitelyNotNullOrNotNull(typeContext, avoidComprehensiveCheck)
        })
    }
    return ConeDefinitelyNotNullType.create(this, typeContext, avoidComprehensiveCheck)
        ?: this.withNullability(nullable = false, typeContext, preserveAttributes = preserveAttributes)
}

fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeTypeProjection>): T {
    if (this.typeArguments === arguments) {
        /**
         * This early return allows to handle [ConeIntersectionType], [ConeTypeVariableType], [ConeStubType],
         * [ConeIntegerLiteralType], [ConeCapturedType], [ConeTypeParameterType], [ConeDynamicType]
         * properly in case when [arguments] is an empty array (no exception arises).
         */
        return this
    }

    fun error(): Nothing = errorWithAttachment("Not supported: ${this::class}") {
        withConeTypeEntry("type", this@withArguments)
    }

    @Suppress("UNCHECKED_CAST")
    return when (val t = this as ConeKotlinType) {
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(t.lookupTag, arguments, isMarkedNullable, attributes) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(t.original.withArguments(arguments)) as T
        is ConeRawType -> ConeRawType.create(t.lowerBound.withArguments(arguments), t.upperBound.withArguments(arguments)) as T
        is ConeDynamicType -> error()
        is ConeFlexibleType -> ConeFlexibleType(t.lowerBound.withArguments(arguments), t.upperBound.withArguments(arguments)) as T
        is ConeErrorType -> ConeErrorType(t.diagnostic, t.isUninferredParameter, typeArguments = arguments, attributes = attributes) as T
        is ConeIntersectionType,
        is ConeTypeVariableType,
        is ConeStubType,
        is ConeIntegerLiteralType,
        is ConeCapturedType,
        is ConeLookupTagBasedType, // ConeLookupTagBasedType is in fact not possible (covered by previous ones)
        -> error()
    }
}

inline fun <T : ConeKotlinType> T.withArguments(replacement: (ConeTypeProjection) -> ConeTypeProjection): T {
    val typeArguments = typeArguments
    return withArguments(Array(typeArguments.size) { replacement(typeArguments[it]) })
}

@OptIn(DynamicTypeConstructor::class)
fun <T : ConeKotlinType> T.withAttributes(attributes: ConeAttributes): T {
    if (this.attributes == attributes) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> ConeErrorType(diagnostic, isUninferredParameter, delegatedType, typeArguments, attributes)
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isMarkedNullable, attributes)
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(original.withAttributes(attributes))
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, isMarkedNullable, attributes)
        is ConeRawType -> ConeRawType.create(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeDynamicType -> ConeDynamicType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withAttributes(attributes), upperBound.withAttributes(attributes))
        is ConeTypeVariableType -> ConeTypeVariableType(isMarkedNullable, typeConstructor, attributes)
        is ConeCapturedType -> copy(attributes = attributes)
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

/**
 * Adds or replaces an `AbbreviatedTypeAttribute`.
 */
fun <T : ConeKotlinType> T.withAbbreviation(attribute: AbbreviatedTypeAttribute): T {
    val clearedAttributes = attributes.abbreviatedType?.let(attributes::remove) ?: attributes
    return withAttributes(clearedAttributes.add(attribute))
}

fun <T : ConeKotlinType> T.withNullabilityOf(
    otherType: ConeKotlinType,
    typeContext: ConeTypeContext
): T {
    if (hasFlexibleMarkedNullability && otherType.hasFlexibleMarkedNullability) return this

    return withNullability(otherType.isMarkedNullable, typeContext)
}

fun <T : ConeKotlinType> T.withNullability(
    nullable: Boolean,
    typeContext: ConeTypeContext,
    attributes: ConeAttributes = this.attributes,
    preserveAttributes: Boolean = false,
): T {
    val theAttributes = attributes.butIf(!preserveAttributes) {
        val withoutEnhanced = it.remove(CompilerConeAttributes.EnhancedNullability)
        withoutEnhanced.transformTypesWith { t -> t.withNullability(nullable, typeContext) } ?: withoutEnhanced
    }

    if (!this.hasFlexibleMarkedNullability && this.isMarkedNullable == nullable && this.attributes == theAttributes && this.lowerBoundIfFlexible() !is ConeIntersectionType) {
        // Note: this is an optimization, but it's not applicable for ConeIntersectionType,
        // because ConeIntersectionType.nullability is always NOT_NULL, independent on real component nullabilities
        // Second note: we can receive a flexible type with intersection types in bounds, so we need unwrap it first
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullable, theAttributes)
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullable, theAttributes)
        is ConeDynamicType -> this
        is ConeFlexibleType -> {
            coneFlexibleOrSimpleType(
                typeContext,
                lowerBound.withNullability(nullable, typeContext, preserveAttributes = preserveAttributes),
                upperBound.withNullability(nullable, typeContext, preserveAttributes = preserveAttributes)
            )
        }

        is ConeTypeVariableType -> ConeTypeVariableType(isMarkedNullable = nullable, typeConstructor, theAttributes)
        is ConeCapturedType -> copy(isMarkedNullable = nullable, attributes = theAttributes)
        is ConeIntersectionType -> when (nullable) {
            true -> this.mapTypes {
                it.withNullability(true, typeContext, preserveAttributes = preserveAttributes)
            }

            false -> if (intersectedTypes.any { !it.isMarkedOrFlexiblyNullable }) this else this.mapTypes {
                it.withNullability(false, typeContext, preserveAttributes = preserveAttributes)
            }
        }

        is ConeStubTypeForTypeVariableInSubtyping -> ConeStubTypeForTypeVariableInSubtyping(constructor, nullable)
        is ConeDefinitelyNotNullType -> when (nullable) {
            false -> this
            true -> original.withNullability(
                true, typeContext, preserveAttributes = preserveAttributes,
            )
        }

        is ConeIntegerLiteralConstantType -> ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, nullable)
        is ConeIntegerConstantOperatorType -> ConeIntegerConstantOperatorTypeImpl(isUnsigned, nullable)
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
        is ConeRigidType -> when (upperBound) {
            is ConeFlexibleType -> coneFlexibleOrSimpleType(typeContext, lowerBound, upperBound.upperBound)
            is ConeRigidType -> when {
                AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound) -> lowerBound
                else -> ConeFlexibleType(lowerBound, upperBound)
            }
        }
    }
}

fun ConeKotlinType.isExtensionFunctionType(session: FirSession): Boolean {
    val type = this.unwrapToSimpleTypeUsingLowerBound().fullyExpandedType(session)
    return type.attributes.extensionFunctionType != null
}

fun FirTypeRef.isExtensionFunctionType(session: FirSession): Boolean {
    return coneTypeSafe<ConeKotlinType>()?.isExtensionFunctionType(session) == true
}

fun FirTypeRef.hasEnhancedNullability(): Boolean =
    coneTypeSafe<ConeKotlinType>()?.hasEnhancedNullability == true

fun FirTypeRef.withoutEnhancedNullability(): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (!hasEnhancedNullability()) return this
    return withReplacedSourceAndType(
        source, coneType.withAttributes(
            ConeAttributes.create(
                this@withoutEnhancedNullability.coneType.attributes.filter { it != CompilerConeAttributes.EnhancedNullability }
            ),
        )
    )
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

    return withReplacedSourceAndType(newSource, newType)
}

internal fun FirResolvedTypeRef.withReplacedSourceAndType(newSource: KtSourceElement?, newType: ConeKotlinType): FirResolvedTypeRef {
    return when {
        newType is ConeErrorType -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = newType.diagnostic
            }
        }
        this is FirErrorTypeRef -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = this@withReplacedSourceAndType.diagnostic
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
            }
        }
        else -> {
            buildResolvedTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
            }
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
    if (containerVisibility == Visibilities.Local) return Visibilities.Local
    return visibility
}


fun ConeTypeContext.captureFromArgumentsInternal(type: ConeRigidType, status: CaptureStatus): ConeRigidType? {
    val capturedArguments = captureArguments(type, status) ?: return null
    return type.withArguments(capturedArguments)
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
        val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableSetOf()) { paramIndex ->
            substitutor.safeSubstitute(
                this as TypeSystemInferenceExtensionContext, parameter.getUpperBound(paramIndex)
            )
        }

        if (oldArgument is ConeKotlinTypeProjectionOut) {
            upperBounds += oldArgument.type
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
            upperBounds.toList() as List<ConeKotlinType>
        }
    }
    return newArguments
}

internal fun ConeTypeContext.captureFromExpressionInternal(type: ConeKotlinType): ConeKotlinType? {
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
    var capturedArgumentsByComponents: List<CapturedArguments> = emptyList()

    // We reuse `TypeToCapture` for some types, suitability to reuse defines by `isSuitableForType`
    fun findCorrespondingCapturedArgumentsForType(type: ConeKotlinType) =
        capturedArgumentsByComponents.find { typeToCapture -> typeToCapture.isSuitableForType(type, this) }?.capturedArguments

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: ConeRigidType): List<ConeKotlinType>? {
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
        is ConeCapturedType -> captureCapturedType(type)
        is ConeDefinitelyNotNullType -> captureFromExpressionInternal(type.original)?.makeConeTypeDefinitelyNotNullOrNotNull(this)
        is ConeFlexibleType -> {
            capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null
            // Flexible types can either have projections in both bounds or just the upper bound (raw types and arrays).
            // Since the scope of flexible types is built from the lower bound, we don't gain any safety from only capturing the
            // upper bound.
            // At the same time, capturing of raw(-like) types leads to issues like KT-63982 or breaks tests like
            // testData/codegen/box/reflection/typeOf/rawTypes_after.kt.
            // Therefore, we return null if nothing was captured for either bound.

            val lowerIntersectedType =
                intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound) ?: return null)
                    .withNullability(type.lowerBound.canBeNull(session), this)
            val upperIntersectedType =
                intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound) ?: return null)
                    .withNullability(type.upperBound.canBeNull(session), this)

            ConeFlexibleType(lowerIntersectedType.coneLowerBoundIfFlexible(), upperIntersectedType.coneUpperBoundIfFlexible())
        }
        is ConeIntersectionType -> {
            capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null
            intersectTypes(
                replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type) ?: return null
            ).withNullability(type.canBeNull(session)) as ConeKotlinType
        }
        is ConeSimpleKotlinType -> {
            captureFromArgumentsInternal(type, CaptureStatus.FROM_EXPRESSION)
        }
    }
}

/**
 * To understand why we need to do capturing on captured types, consider the following case:
 *
 * ```kt
 * class Box<T>(val value: T)
 * interface Foo<T>
 *
 * fun test(x: Box<out Foo<*>>) {
 *     someCall(x.value)
 * }
 * ```
 *
 * The type of `x.value` is `CapturedType(out Foo<*>)`.
 * Note that capturing only applies to the top level, i.e., nested projections are not captured.
 *
 * When we use the expression with type `CapturedType(out Foo<*>)` as an argument of another call,
 * it becomes necessary to support capturing of captured types,
 * otherwise the star projection in `CapturedType(out Foo<*>)` is not properly captured.
 *
 * The method is a version of [org.jetbrains.kotlin.fir.resolve.substitution.substitute] specifically for capturing
 * that doesn't have the issue of KT-64024 where nothing is done when neither [ConeCapturedType.lowerType]
 * nor [ConeCapturedTypeConstructor.projection] need capturing.
 */
private fun ConeTypeContext.captureCapturedType(type: ConeCapturedType): ConeCapturedType? {
    val capturedProjection = type.constructor.projection.type
        ?.let { captureFromExpressionInternal(it) }
        ?.let { wrapProjection(type.constructor.projection, it) }
    val capturedSuperTypes = type.constructor.supertypes?.map { captureFromExpressionInternal(it) ?: it }
    val capturedLowerType = type.lowerType?.let { captureFromExpressionInternal(it) }

    if (capturedProjection == null && capturedLowerType == null && capturedSuperTypes == type.constructor.supertypes) {
        return null
    }

    return type.copy(
        constructor = ConeCapturedTypeConstructor(
            projection = capturedProjection ?: type.constructor.projection,
            supertypes = capturedSuperTypes,
            typeParameterMarker = type.constructor.typeParameterMarker
        ),
        lowerType = capturedLowerType ?: type.lowerType,
    )
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

fun ConeKotlinType.canHaveSubtypesAccordingToK1(session: FirSession): Boolean =
    hasSubtypesAboveNothingAccordingToK1(session)

/**
 * The original K1 function: [org.jetbrains.kotlin.types.TypeUtils.canHaveSubtypes].
 */
private fun ConeKotlinType.hasSubtypesAboveNothingAccordingToK1(session: FirSession): Boolean {
    val expandedType = fullyExpandedType(session)
    if (expandedType.isMarkedNullable) {
        return true
    }
    val classSymbol = expandedType.toClassSymbol(session) ?: return true
    // In K2 enum classes are final, though enum entries are their subclasses (which is a compiler implementation detail).
    if (classSymbol.isEnumClass || classSymbol.isExpect || classSymbol.modality != Modality.FINAL) {
        return true
    }

    classSymbol.typeParameterSymbols.forEachIndexed { idx, typeParameterSymbol ->
        val typeProjection = expandedType.typeArguments[idx]

        if (typeProjection.isStarProjection) {
            return true
        }

        val argument = typeProjection.type!! //safe because it is not a star

        val canHaveSubtypes = when (typeProjection.variance) {
            Variance.OUT_VARIANCE -> argument.hasSubtypesAboveNothingAccordingToK1(session)
            Variance.IN_VARIANCE -> argument.hasSupertypesBelowParameterBoundsAccordingToK1(typeParameterSymbol, session)
            Variance.INVARIANT -> when (typeParameterSymbol.variance) {
                Variance.OUT_VARIANCE -> argument.hasSubtypesAboveNothingAccordingToK1(session)
                Variance.IN_VARIANCE -> argument.hasSupertypesBelowParameterBoundsAccordingToK1(typeParameterSymbol, session)
                Variance.INVARIANT -> argument.hasSubtypesAboveNothingAccordingToK1(session)
                        || argument.hasSupertypesBelowParameterBoundsAccordingToK1(typeParameterSymbol, session)
            }
        }

        if (canHaveSubtypes) {
            return true
        }
    }

    return false
}

/**
 * The original K1 function: [org.jetbrains.kotlin.types.TypeUtils.lowerThanBound].
 * This function returns `true` if `argument` suits any bound rather than the
 * intersection of them all, and it expects there to be at least a single bound.
 */
private fun ConeKotlinType.hasSupertypesBelowParameterBoundsAccordingToK1(
    typeParameterSymbol: FirTypeParameterSymbol,
    session: FirSession,
): Boolean {
    typeParameterSymbol.resolvedBounds.forEach { boundTypeRef ->
        if (this != boundTypeRef.coneType && isSubtypeOf(session.typeContext, boundTypeRef.coneType)) {
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

fun List<FirTypeParameterSymbol>.getProjectionsForRawType(session: FirSession, nullabilities: BooleanArray?): Array<ConeKotlinType> {
    val cache = mutableMapOf<FirTypeParameter, ConeKotlinType>()
    return Array(size) { index ->
        this[index].getProjectionForRawType(session, cache, nullabilities?.get(index) == true)
    }
}

fun FirTypeParameterSymbol.getProjectionForRawType(
    session: FirSession,
    makeNullable: Boolean,
): ConeKotlinType {
    return getProjectionForRawType(session, mutableMapOf(), makeNullable)
}

private fun FirTypeParameterSymbol.getProjectionForRawType(
    session: FirSession,
    cache: MutableMap<FirTypeParameter, ConeKotlinType>,
    makeNullable: Boolean,
): ConeKotlinType {
    return fir.eraseToUpperBound(session, cache, mode = EraseUpperBoundMode.FOR_RAW_TYPE_ERASURE)
        .applyIf(makeNullable) {
            withNullability(nullable = true, session.typeContext)
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
                else -> session.builtinTypes.anyType.coneType
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
        val type = typeArgument.getType() ?: return@replaceArgumentsDeeply typeArgument
        val typeConstructor = type.typeConstructor().takeIf { it.isTypeParameterTypeConstructor() }
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
                if (isMarkedNullable) it.withNullability(nullable = true, session.typeContext) else it
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

/**
 * Returns true if this type can be `null`.
 * This function expands typealiases, checks upper bounds of type parameters, the components of intersection types, etc.
 */
fun ConeKotlinType.canBeNull(session: FirSession): Boolean {
    return when (this) {
        is ConeFlexibleType -> upperBound.canBeNull(session)
        is ConeDefinitelyNotNullType -> false
        is ConeTypeParameterType -> isMarkedNullable || this.lookupTag.typeParameterSymbol.resolvedBounds.all {
            it.coneType.canBeNull(session)
        }
        is ConeStubType -> {
            isMarkedNullable ||
                    (constructor.variable.defaultType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.symbol.let {
                        it == null || it.allBoundsAreNullableOrUnresolved(session)
                    }
        }
        is ConeIntersectionType -> intersectedTypes.all { it.canBeNull(session) }
        is ConeCapturedType -> isMarkedNullable || constructor.supertypes?.all { it.canBeNull(session) } == true
        is ConeErrorType -> diagnostic.let { it !is ConeDiagnosticWithNullability || it.isNullable }
        is ConeLookupTagBasedType -> isMarkedNullable || fullyExpandedType(session).isMarkedNullable
        is ConeIntegerLiteralType, is ConeTypeVariableType -> isMarkedNullable
    }
}

private fun FirTypeParameterSymbol.allBoundsAreNullableOrUnresolved(session: FirSession): Boolean {
    for (bound in fir.bounds) {
        if (bound !is FirResolvedTypeRef) return true
        if (!bound.coneType.canBeNull(session)) return false
    }

    return true
}

fun FirIntersectionTypeRef.isLeftValidForDefinitelyNotNullable(session: FirSession): Boolean =
    leftType.coneType.let { it is ConeTypeParameterType && it.canBeNull(session) && !it.isMarkedNullable }

val FirIntersectionTypeRef.isRightValidForDefinitelyNotNullable: Boolean get() = rightType.coneType.isAny

fun ConeKotlinType.isKCallableType(): Boolean {
    return this.classId == StandardClassIds.KCallable
}

val ConeKotlinType.isUnitOrFlexibleUnit: Boolean
    get() {
        val type = this.lowerBoundIfFlexible()
        if (type.isMarkedNullable) return false
        val classId = type.classId ?: return false
        return classId == StandardClassIds.Unit
    }

fun ConeClassLikeLookupTag.isLocalClass(): Boolean {
    return classId.isLocal
}

fun ConeClassLikeLookupTag.isAnonymousClass(): Boolean {
    return name == SpecialNames.ANONYMOUS
}

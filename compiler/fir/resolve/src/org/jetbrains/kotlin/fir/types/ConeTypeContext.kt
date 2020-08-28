/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.correspondingSupertypesCache
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.*

class ErrorTypeConstructor(val reason: String) : TypeConstructorMarker {
    override fun toString(): String = reason
}

interface ConeTypeContext : TypeSystemContext, TypeSystemOptimizationContext, TypeCheckerProviderContext, TypeSystemCommonBackendContext {
    val session: FirSession

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        return this is ConeIntegerLiteralType
    }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        return (this as? ConeIntegerLiteralType)?.possibleTypes ?: emptyList()
    }

    override fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? {
        require(this is ConeKotlinType)
        return session.correspondingSupertypesCache.getCorrespondingSupertypes(this, constructor)
    }

    override fun SimpleTypeMarker.isIntegerLiteralType(): Boolean {
        return this is ConeIntegerLiteralType
    }

    override fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker? {
        assert(this is ConeKotlinType)
        return when (this) {
            is ConeClassLikeType -> fullyExpandedType(session)
            is ConeSimpleKotlinType -> this
            is ConeFlexibleType -> null
            else -> error("Unknown simpleType: $this")
        }
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        assert(this is ConeKotlinType)
        return this as? ConeFlexibleType
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        assert(this is ConeKotlinType)
        return this is ConeClassErrorType || this is ConeKotlinErrorType || this.typeConstructor().isError()
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean {
        assert(this is ConeKotlinType)
        return false // TODO
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        assert(this is ConeKotlinType)
        return null // TODO
    }

    override fun FlexibleTypeMarker.asRawType(): RawTypeMarker? {
        require(this is ConeFlexibleType)
        return this as? ConeRawType
    }

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is ConeFlexibleType)
        return this.upperBound as SimpleTypeMarker
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is ConeFlexibleType)
        return this.lowerBound as SimpleTypeMarker
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        //require(this is ConeLookupTagBasedType)
        return this as? ConeCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        require(this is ConeKotlinType)
        return this as? ConeDefinitelyNotNullType
    }

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean {
        require(this is ConeKotlinType)
        return this.nullability.isNullable
    }

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return withNullability(ConeNullability.create(nullable), this as? ConeInferenceContext)
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        return when (this) {
            is ConeClassLikeType -> lookupTag
            is ConeTypeParameterType -> lookupTag
            is ConeCapturedType -> constructor
            is ConeTypeVariableType -> lookupTag as ConeTypeVariableTypeConstructor // TODO: WTF
            is ConeIntersectionType -> this
            is ConeStubType -> variable.typeConstructor
            is ConeDefinitelyNotNullType -> original.typeConstructor()
            is ConeIntegerLiteralType -> this
            else -> error("?: $this")
        }
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is ConeCapturedType)
        return this.constructor
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.captureStatus
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        require(this is ConeCapturedTypeConstructor)
        return this.projection
    }

    override fun KotlinTypeMarker.argumentsCount(): Int {
        require(this is ConeKotlinType)

        return this.typeArguments.size
    }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is ConeKotlinType)

        return this.typeArguments.getOrNull(index)
            ?: session.builtinTypes.anyType.type//StandardClassIds.Any(session.firSymbolProvider).constructType(emptyArray(), false) // TODO wtf
    }

    override fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is ConeKotlinType)

        return this
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
        require(this is ConeCapturedType)
        return this.lowerType
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        require(this is ConeTypeProjection)
        return this is ConeStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is ConeTypeProjection)

        return when (this.kind) {
            ProjectionKind.STAR -> error("Nekorrektno (c) Stas")
            ProjectionKind.IN -> TypeVariance.IN
            ProjectionKind.OUT -> TypeVariance.OUT
            ProjectionKind.INVARIANT -> TypeVariance.INV
        }
    }

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker {
        require(this is ConeTypeProjection)
        require(this is ConeKotlinTypeProjection) { "No type for StarProjection" }
        return this.type
    }

    override fun TypeConstructorMarker.parametersCount(): Int {
        //require(this is ConeSymbol)
        return when (this) {
            is ConeTypeParameterLookupTag,
            is ConeCapturedTypeConstructor,
            is ErrorTypeConstructor,
            is ConeTypeVariableTypeConstructor,
            is ConeIntersectionType -> 0
            is ConeClassLikeLookupTag -> {
                when(val symbol = toSymbol(session)) {
                    is FirAnonymousObjectSymbol -> symbol.fir.typeParameters.size
                    is FirRegularClassSymbol -> symbol.fir.typeParameters.size
                    is FirTypeAliasSymbol -> symbol.fir.typeParameters.size
                    else -> 0
                }
            }
            is ConeIntegerLiteralType -> 0
            else -> error("?!:10")
        }
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        //require(this is ConeSymbol)
        return when (val symbol = toClassLikeSymbol()) {
            is FirAnonymousObjectSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            is FirRegularClassSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            is FirTypeAliasSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            else -> error("?!:12")
        }
    }

    private fun TypeConstructorMarker.toClassLikeSymbol(): FirClassLikeSymbol<*>? = (this as? ConeClassLikeLookupTag)?.toSymbol(session)

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        if (this is ErrorTypeConstructor) return emptyList()
        //require(this is ConeSymbol)
        return when (this) {
            is ConeTypeVariableTypeConstructor -> emptyList()
            is ConeTypeParameterLookupTag -> symbol.fir.bounds.map { it.coneType }
            is ConeClassLikeLookupTag -> {
                when (val symbol = toClassLikeSymbol()) {
                    is FirClassSymbol<*> -> symbol.fir.superConeTypes
                    is FirTypeAliasSymbol -> listOfNotNull(symbol.fir.expandedConeType)
                    else -> emptyList()
                }
            }
            is ConeCapturedTypeConstructor -> supertypes!!
            is ConeIntersectionType -> intersectedTypes
            is ConeIntegerLiteralType -> supertypes
            else -> error("?!:13")
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return this is ConeIntersectionType
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        //assert(this is ConeSymbol)
        return this is ConeClassLikeLookupTag
    }

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is ConeTypeParameterLookupTag)
        return this.symbol.fir.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is ConeTypeParameterLookupTag)
        return this.symbol.fir.bounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker {
        require(this is ConeTypeParameterLookupTag)
        return this.symbol.fir.bounds[index].coneType
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeParameterLookupTag)
        return this
    }

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (c1 is ErrorTypeConstructor || c2 is ErrorTypeConstructor) return false

        //assert(c1 is ConeSymbol)
        //assert(c2 is ConeSymbol)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isDenotable(): Boolean {
        //TODO
        return when (this) {
            is ConeCapturedTypeConstructor,
            is ConeTypeVariableTypeConstructor,
            is ConeIntersectionType,
            is ConeIntegerLiteralType -> false
            is AbstractFirBasedSymbol<*> -> true
            else -> true
        }
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        val symbol = toClassLikeSymbol() ?: return false
        if (symbol is FirAnonymousObjectSymbol) return true
        val classSymbol = symbol as? FirRegularClassSymbol ?: return false
        val fir = classSymbol.fir
        return fir.modality == Modality.FINAL &&
                fir.classKind != ClassKind.ENUM_ENTRY &&
                fir.classKind != ClassKind.ANNOTATION_CLASS
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is ConeKotlinType)
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

        for (index in 0 until argumentsCount) {
            val oldArgument = type.typeArguments[index]
            val newArgument = newArguments[index]

            if (oldArgument !is ConeStarProjection && oldArgument.kind == ProjectionKind.INVARIANT) continue

            val parameter = typeConstructor.getParameter(index)
            val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableListOf()) { paramIndex ->
                parameter.getUpperBound(paramIndex) // TODO: substitution
            }

            if (!oldArgument.isStarProjection() && oldArgument.getVariance() == TypeVariance.OUT) {
                upperBounds += oldArgument.getType()
            }

            require(newArgument is ConeCapturedType)
            @Suppress("UNCHECKED_CAST")
            newArgument.constructor.supertypes = upperBounds as List<ConeKotlinType>
        }

        return type.withArguments(newArguments)
    }

    override fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is ConeKotlinType)
        return this
    }

    override fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        require(a is ConeKotlinType)
        require(b is ConeKotlinType)
        return a.typeArguments === b.typeArguments
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StandardClassIds.Any
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StandardClassIds.Nothing
    }

    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        if (isError()) return false
        if (this is ConeCapturedType) return true
        if (this is ConeTypeVariableType) return false
        if (this is ConeIntersectionType) return false
        if (this is ConeIntegerLiteralType) return true
        if (this is ConeStubType) return true
        if (this is ConeDefinitelyNotNullType) return true
        require(this is ConeLookupTagBasedType)
        val typeConstructor = this.typeConstructor()
        return typeConstructor is ConeClassLikeLookupTag ||
                typeConstructor is ConeTypeParameterLookupTag
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        TODO("not implemented")
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        if (this is ConeClassLikeType) {
            return StandardClassIds.primitiveTypes.contains(this.lookupTag.classId)
        }
        return false
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        return this is StubTypeMarker
    }

    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return ConeTypeIntersector.intersectTypes(this as ConeInferenceContext, types as List<ConeKotlinType>) as SimpleTypeMarker
    }

    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return ConeTypeIntersector.intersectTypes(this as ConeInferenceContext, types as List<ConeKotlinType>)
    }

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return when (type) {
            is ConeClassLikeType -> type.fullyExpandedType(session)
            is ConeFlexibleType -> {
                val lowerBound = prepareType(type.lowerBound)
                if (lowerBound === type.lowerBound) return type

                ConeFlexibleType(
                    lowerBound as ConeKotlinType,
                    prepareType(type.upperBound) as ConeKotlinType
                )
            }
            else -> type
        }
    }

    override fun KotlinTypeMarker.isNullableType(): Boolean {
        require(this is ConeKotlinType)
        if (this.isMarkedNullable)
            return true

        if (this is ConeFlexibleType && this.upperBound.isNullableType())
            return true

        if (this is ConeTypeParameterType /* || is TypeVariable */)
            return hasNullableSuperType(type)

        if (this is ConeIntersectionType && intersectedTypes.any { it.isNullableType() }) {
            return true
        }

        return false
    }

    private fun TypeConstructorMarker.toFirRegularClass(): FirRegularClass? {
        return toClassLikeSymbol()?.fir as? FirRegularClass
    }

    override fun nullableAnyType(): SimpleTypeMarker = TODO("not implemented")

    override fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker = TODO("not implemented")

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean = TODO("not implemented")

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        val firRegularClass = toFirRegularClass() ?: return false

        return firRegularClass.modality == Modality.FINAL ||
                firRegularClass.classKind == ClassKind.ENUM_ENTRY ||
                firRegularClass.classKind == ClassKind.ANNOTATION_CLASS
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        // TODO support annotations
        return false
    }

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
        // TODO support annotations
        return null
    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        return this as? ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.isInlineClass(): Boolean {
        return toFirRegularClass()?.isInline == true
    }

    override fun TypeConstructorMarker.isInnerClass(): Boolean {
        return toFirRegularClass()?.isInner == true
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker {
        require(this is ConeTypeParameterLookupTag)
        return this.symbol.fir.bounds.getOrNull(0)?.coneType
            ?: session.builtinTypes.nullableAnyType.type
    }

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? {
        // TODO: support inline classes
        return null
    }

    override fun TypeConstructorMarker.getPrimitiveType() =
        getClassFqNameUnsafe()?.let(StandardNames.FqNames.fqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.getPrimitiveArrayType() =
        getClassFqNameUnsafe()?.let(StandardNames.FqNames.arrayClassFqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.isUnderKotlinPackage() =
        getClassFqNameUnsafe()?.startsWith(Name.identifier("kotlin")) == true

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? {
        if (this !is ConeClassLikeLookupTag) return null
        return classId.asSingleFqName().toUnsafe()
    }

    override fun TypeParameterMarker.getName() = (this as ConeTypeParameterLookupTag).name

    override fun TypeParameterMarker.isReified(): Boolean = TODO("not implemented")

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val classKind = typeConstructor().toFirRegularClass()?.classKind ?: return false
        return classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.INTERFACE
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        return this is ErrorTypeConstructor || (this is ConeClassLikeLookupTag && this.toSymbol(session) == null)
    }
}

class ConeTypeCheckerContext(
    override val isErrorTypeEqualsToAnything: Boolean,
    override val isStubTypeEqualsToAnything: Boolean,
    override val session: FirSession
) : AbstractTypeCheckerContext(), ConeInferenceContext {
    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy {
        if (type.argumentsCount() == 0) return SupertypesPolicy.LowerIfFlexible
        require(type is ConeKotlinType)
        val declaration = when (type) {
            is ConeClassLikeType -> type.lookupTag.toSymbol(session)?.firUnsafe<FirClassLikeDeclaration<*>>()
            else -> null
        }

        val substitutor = if (declaration is FirTypeParameterRefsOwner) {
            val substitution =
                declaration.typeParameters.zip(type.typeArguments).associate { (parameter, argument) ->
                    parameter.symbol to ((argument as? ConeKotlinTypeProjection)?.type
                        ?: session.builtinTypes.nullableAnyType.type)//StandardClassIds.Any(session.firSymbolProvider).constructType(emptyArray(), isNullable = true))
                }
            substitutorByMap(substitution)
        } else {
            ConeSubstitutor.Empty
        }
        return object : SupertypesPolicy.DoCustomTransform() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): SimpleTypeMarker {
                val lowerBound = type.lowerBoundIfFlexible()
                require(lowerBound is ConeKotlinType)
                return substitutor.substituteOrSelf(lowerBound) as SimpleTypeMarker
            }

        }
    }

    override fun areEqualTypeConstructors(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean {
        return a == b
    }

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return super<ConeInferenceContext>.prepareType(type)
    }

    override fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
        return prepareType(type)
    }

    override val KotlinTypeMarker.isAllowedTypeVariable: Boolean
        get() = this is ConeKotlinType && this is ConeTypeVariableType

    override fun newBaseTypeCheckerContext(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): AbstractTypeCheckerContext =
        if (this.isErrorTypeEqualsToAnything == errorTypesEqualToAnything)
            this
        else
            ConeTypeCheckerContext(errorTypesEqualToAnything, stubTypesEqualToAnything, session)

    override fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker
    ): KotlinTypeMarker {
        // TODO
        return firstCandidate
    }
}

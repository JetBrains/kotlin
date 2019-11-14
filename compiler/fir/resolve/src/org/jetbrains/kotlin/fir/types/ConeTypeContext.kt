/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.hasNullableSuperType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.model.*

class ErrorTypeConstructor(val reason: String) : TypeConstructorMarker {
    override fun toString(): String = reason
}

interface ConeTypeContext : TypeSystemContext, TypeSystemOptimizationContext, TypeCheckerProviderContext, TypeSystemCommonBackendContext {
    val session: FirSession

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        // TODO()
        return false
    }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        TODO("not implemented")
    }

    override fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? {
        require(this is ConeKotlinType)
        return session.correspondingSupertypesCache.getCorrespondingSupertypes(this, constructor)
    }

    override fun SimpleTypeMarker.isIntegerLiteralType(): Boolean {
        return false
    }

    override fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker? {
        assert(this is ConeKotlinType)
        return when (this) {
            is ConeAbbreviatedType -> directExpansionType(session)
                ?: ConeClassErrorType("no expansion for type-alias: ${this.abbreviationLookupTag.classId}")
            is ConeCapturedType -> this
            is ConeLookupTagBasedType -> this
            is ConeDefinitelyNotNullType -> this
            is ConeIntersectionType -> this
            is ConeFlexibleType -> null
            is ConeStubType -> this
            else -> error("Unknown simpleType: $this")
        }
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        assert(this is ConeKotlinType)
        return this as? ConeFlexibleType
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        assert(this is ConeKotlinType)
        return this is ConeClassErrorType || this is ConeKotlinErrorType || this.typeConstructor() is ErrorTypeConstructor
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
        assert(this is ConeKotlinType)
        return null // TODO
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
        return null // TODO
    }

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean {
        require(this is ConeKotlinType)
        return this.nullability.isNullable
    }

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return withNullability(ConeNullability.create(nullable))
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        val typeConstructor = when (this) {
            is ConeCapturedType -> constructor
            is ConeTypeVariableType -> this.lookupTag as ConeTypeVariableTypeConstructor // TODO: WTF
            is ConeAbbreviatedType -> this.directExpansionType(session)?.typeConstructor()
                ?: ErrorTypeConstructor("Failed to expand alias: ${this}")
            is ConeLookupTagBasedType -> this.lookupTag.toSymbol(session) ?: ErrorTypeConstructor("Unresolved: ${this.lookupTag}")
            is ConeIntersectionType -> this
            is ConeStubType -> variable.typeConstructor
            else -> error("?: ${this}")
        }

        // TODO: get rid of class types with type-alias symbols
        if (typeConstructor is FirTypeAliasSymbol) {
            return typeConstructor.fir.expandedTypeRef.coneTypeSafe<ConeKotlinType>()?.typeConstructor()
                ?: ErrorTypeConstructor("Failed to expand alias: ${this}")
        }
        return typeConstructor
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
            ?: StandardClassIds.Any(session.firSymbolProvider).constructType(emptyArray(), false) // TODO wtf
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
        require(this is ConeKotlinTypeProjection)
        return this is ConeStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is ConeKotlinTypeProjection)

        return when (this.kind) {
            ProjectionKind.STAR -> error("Nekorrektno (c) Stas")
            ProjectionKind.IN -> TypeVariance.IN
            ProjectionKind.OUT -> TypeVariance.OUT
            ProjectionKind.INVARIANT -> TypeVariance.INV
        }
    }

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker {
        require(this is ConeKotlinTypeProjection)
        require(this is ConeTypedProjection) { "No type for StarProjection" }
        return this.type
    }

    override fun TypeConstructorMarker.parametersCount(): Int {
        //require(this is ConeSymbol)
        return when (this) {
            is FirTypeParameterSymbol,
            is FirAnonymousObjectSymbol,
            is ConeCapturedTypeConstructor,
            is ErrorTypeConstructor,
            is ConeTypeVariableTypeConstructor,
            is ConeIntersectionType -> 0
            is FirRegularClassSymbol -> fir.typeParameters.size
            is FirTypeAliasSymbol -> fir.typeParameters.size
            else -> error("?!:10")
        }
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        //require(this is ConeSymbol)
        return when (this) {
            is FirTypeParameterSymbol -> error("?!:11")
            is FirRegularClassSymbol -> fir.typeParameters[index].symbol
            is FirTypeAliasSymbol -> fir.typeParameters[index].symbol
            else -> error("?!:12")
        }
    }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        if (this is ErrorTypeConstructor) return emptyList()
        //require(this is ConeSymbol)
        return when (this) {
            is ConeTypeVariableTypeConstructor -> emptyList()
            is FirTypeParameterSymbol -> fir.bounds.map { it.coneTypeUnsafe() }
            is FirClassSymbol<*> -> fir.superConeTypes
            is FirTypeAliasSymbol -> listOfNotNull(fir.expandedConeType)
            is ConeCapturedTypeConstructor -> supertypes!!
            is ConeIntersectionType -> intersectedTypes
            else -> error("?!:13")
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return this is ConeIntersectionType
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        //assert(this is ConeSymbol)
        return this is FirClassSymbol<*>
    }

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is FirTypeParameterSymbol)
        return this.fir.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is FirTypeParameterSymbol)
        return this.fir.bounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker {
        require(this is FirTypeParameterSymbol)
        return this.fir.bounds[index].coneTypeUnsafe()
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is FirTypeParameterSymbol)
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
            is ConeIntersectionType -> false
            is AbstractFirBasedSymbol<*> -> true
            else -> true
        }
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        if (this is FirAnonymousObjectSymbol) return true
        val classSymbol = this as? FirRegularClassSymbol ?: return false
        val fir = classSymbol.fir
        return fir.modality == Modality.FINAL &&
                fir.classKind != ClassKind.ENUM_ENTRY &&
                fir.classKind != ClassKind.ANNOTATION_CLASS
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is ConeKotlinType)
        val argumentsCount = type.argumentsCount()
        if (argumentsCount == 0) return null

        val typeConstructor = type.typeConstructor()
        if (argumentsCount != typeConstructor.parametersCount()) return null

        if (type.asArgumentList().all(this) { !it.isStarProjection() && it.getVariance() == TypeVariance.INV }) return null
        val newArguments = Array(argumentsCount) { index ->
            val argument = type.getArgument(index)
            if (!argument.isStarProjection() && argument.getVariance() == TypeVariance.INV) return@Array argument as ConeKotlinTypeProjection

            val lowerType = if (!argument.isStarProjection() && argument.getVariance() == TypeVariance.IN) {
                argument.getType() as ConeKotlinType
            } else {
                null
            }

            ConeCapturedType(status, lowerType, argument as ConeKotlinTypeProjection)
        }

        for (index in 0 until argumentsCount) {
            val oldArgument = type.getArgument(index)
            val newArgument = newArguments[index]

            if (!oldArgument.isStarProjection() && oldArgument.getVariance() == TypeVariance.INV) continue

            val parameter = typeConstructor.getParameter(index)
            val upperBounds = (0 until parameter.upperBoundCount()).mapTo(mutableListOf()) { paramIndex ->
                parameter.getUpperBound(paramIndex) // TODO: substitution
            }

            if (!oldArgument.isStarProjection() && oldArgument.getVariance() == TypeVariance.OUT) {
                upperBounds += oldArgument.getType()
            }

            require(newArgument is ConeCapturedType)
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
        return this is FirClassLikeSymbol<*> && classId == StandardClassIds.Any
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is FirClassLikeSymbol<*> && classId == StandardClassIds.Nothing
    }

    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        if (isError()) return false
        if (this is ConeCapturedType) return true
        if (this is ConeTypeVariableType) return false
        if (this is ConeIntersectionType) return false
        if (this is ConeStubType) return true
        require(this is ConeLookupTagBasedType)
        val typeConstructor = this.typeConstructor()
        return typeConstructor is FirClassSymbol<*> ||
                typeConstructor is FirTypeParameterSymbol
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        TODO("not implemented")
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        return false //TODO
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

    private fun prepareClassLikeType(
        type: ConeClassLikeType,
        visited: MutableSet<ConeAbbreviatedType>
    ): KotlinTypeMarker {
        return when (type) {
            is ConeAbbreviatedType -> prepareAbbreviatedType(type, visited)
            else -> type
        }
    }

    private fun prepareAbbreviatedType(
        type: ConeAbbreviatedType,
        visited: MutableSet<ConeAbbreviatedType> = mutableSetOf()
    ): KotlinTypeMarker {
        if (type in visited) return ConeClassErrorType("Recursive type alias")
        visited += type
        return prepareClassLikeType(type.directExpansionType(session) ?: ConeClassErrorType("unresolved"), visited)
    }

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return when (type) {
            is ConeAbbreviatedType -> prepareAbbreviatedType(type)
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

        // TODO: Intersection types
        return false
    }

    private fun TypeConstructorMarker.toFirRegularClass(): FirRegularClass? {
        if (this !is FirClassLikeSymbol<*>) return null
        return fir as? FirRegularClass
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
        return this as? FirTypeParameterSymbol
    }

    override fun TypeConstructorMarker.isInlineClass(): Boolean {
        return toFirRegularClass()?.isInline == true
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker {
        require(this is FirTypeParameterSymbol)
        return this.fir.bounds.getOrNull(0)?.let { (it as? FirResolvedTypeRef)?.type }
            ?: ConeClassTypeImpl(
                ConeClassLikeLookupTagImpl(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.any.toSafe())), emptyArray(), true
            )
    }

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? {
        // TODO: support inline classes
        return null
    }

    override fun TypeConstructorMarker.getPrimitiveType() =
        getClassFqNameUnsafe()?.let(KotlinBuiltIns.FQ_NAMES.fqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.getPrimitiveArrayType() =
        getClassFqNameUnsafe()?.let(KotlinBuiltIns.FQ_NAMES.arrayClassFqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.isUnderKotlinPackage() =
        getClassFqNameUnsafe()?.startsWith(Name.identifier("kotlin")) == true

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? {
        return toFirRegularClass()?.symbol?.toLookupTag()?.classId?.asSingleFqName()?.toUnsafe()
    }

    override fun TypeParameterMarker.getName() = (this as FirTypeParameterSymbol).name

    override fun TypeParameterMarker.isReified(): Boolean = TODO("not implemented")

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val classKind = typeConstructor().toFirRegularClass()?.classKind ?: return false
        return classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.INTERFACE
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        return this is ErrorTypeConstructor
    }
}

class ConeTypeCheckerContext(
    override val isErrorTypeEqualsToAnything: Boolean,
    override val isStubTypeEqualsToAnything: Boolean,
    override val session: FirSession
) : AbstractTypeCheckerContext(), ConeTypeContext {
    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy {
        if (type.argumentsCount() == 0) return SupertypesPolicy.LowerIfFlexible
        require(type is ConeKotlinType)
        val declaration = when (type) {
            is ConeClassType -> type.lookupTag.toSymbol(session)?.firUnsafe<FirRegularClass>()
            else -> null
        }

        val substitutor = if (declaration != null) {
            val substitution =
                declaration.typeParameters.zip(type.typeArguments).associate { (parameter, argument) ->
                    parameter.symbol to ((argument as? ConeTypedProjection)?.type
                        ?: StandardClassIds.Any(session.firSymbolProvider).constructType(emptyArray(), isNullable = true))
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
        return super<ConeTypeContext>.prepareType(type)
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

}

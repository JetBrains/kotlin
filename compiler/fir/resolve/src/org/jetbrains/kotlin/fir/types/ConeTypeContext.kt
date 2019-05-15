/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.declarations.superConeTypes
import org.jetbrains.kotlin.fir.resolve.calls.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.resolve.withArguments
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.model.*


class ErrorTypeConstructor(reason: String) : TypeConstructorMarker

interface ConeTypeContext : TypeSystemContext, TypeSystemOptimizationContext {
    val session: FirSession

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        // TODO()
        return false
    }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        TODO("not implemented")
    }

    override fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker? {
        assert(this is ConeKotlinType)
        return when (this) {
            is ConeAbbreviatedType -> directExpansionType(session)
                ?: ConeClassErrorType("no expansion for type-alias: ${this.abbreviationLookupTag.classId}")
            is ConeCapturedType -> this
            is ConeLookupTagBasedType -> this
            is ConeDefinitelyNotNullType -> this
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
        if (nullability.isNullable == nullable) return this
        return when (this) {
            is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, ConeNullability.create(nullable), constructor)
            is ConeTypeParameterType -> ConeTypeParameterTypeImpl(lookupTag, nullable)
            is ConeClassErrorType -> this
            is ConeClassType -> ConeClassTypeImpl(lookupTag, typeArguments, nullable)
            is ConeAbbreviatedType -> ConeAbbreviatedTypeImpl(
                lookupTag,
                typeArguments,
                nullable
            )
            else -> error("!")
        }
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        val typeConstructor = when (this) {
            is ConeCapturedType -> constructor
            is ConeTypeVariableType -> this.lookupTag as ConeTypeVariableTypeConstructor // TODO: WTF
            is ConeAbbreviatedType -> this.directExpansionType(session)?.typeConstructor()
                ?: ErrorTypeConstructor("Failed to expand alias: ${this}")
            is ConeLookupTagBasedType -> this.lookupTag.toSymbol(session) ?: ErrorTypeConstructor("Unresolved: ${this.lookupTag}")
            else -> error("?: ${this}")
        }

        // TODO: get rid of class types with type-alias symbols
        if (typeConstructor is FirTypeAliasSymbol) {
            return typeConstructor.fir.expandedTypeRef.coneTypeSafe<ConeKotlinType>()?.typeConstructor()
                ?: ErrorTypeConstructor("Failed to expand alias: ${this}")
        }
        return typeConstructor
    }

    override fun SimpleTypeMarker.argumentsCount(): Int {
        require(this is ConeKotlinType)

        return this.typeArguments.size
    }

    override fun SimpleTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is ConeKotlinType)

        return this.typeArguments.getOrNull(index)
            ?: StandardClassIds.Any(session.service()).constructType(emptyArray(), false) // TODO wtf
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
            is ConeTypeParameterSymbol, is ConeCapturedTypeConstructor, is ErrorTypeConstructor, is ConeTypeVariableTypeConstructor -> 0
            is FirClassSymbol -> fir.typeParameters.size
            is FirTypeAliasSymbol -> fir.typeParameters.size
            else -> error("?!:10")
        }
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        //require(this is ConeSymbol)
        return when (this) {
            is ConeTypeParameterSymbol -> error("?!:11")
            is FirClassSymbol -> fir.typeParameters[index].symbol
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
            is FirClassSymbol -> fir.superConeTypes
            is FirTypeAliasSymbol -> listOfNotNull(fir.expandedConeType)
            is ConeCapturedTypeConstructor -> supertypes!!
            else -> error("?!:13")
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return false // TODO
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        //assert(this is ConeSymbol)
        return this is FirClassSymbol
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
            is ConeCapturedTypeConstructor -> false
            is ConeTypeVariableTypeConstructor -> false
            is ConeSymbol -> true
            else -> true
        }
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        val classSymbol = this as? ConeClassSymbol ?: return false
        val fir = (classSymbol as FirClassSymbol).fir
        return fir.modality == Modality.FINAL &&
                fir.classKind != ClassKind.ENUM_ENTRY &&
                fir.classKind != ClassKind.ANNOTATION_CLASS
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is ConeKotlinType)
        val typeConstructor = type.typeConstructor()
        if (type.argumentsCount() != typeConstructor.parametersCount()) return null
        if (type.asArgumentList().all(this) { !it.isStarProjection() && it.getVariance() == TypeVariance.INV }) return null
        val newArguments = Array(type.argumentsCount()) { index ->
            val argument = type.getArgument(index)
            if (!argument.isStarProjection() && argument.getVariance() == TypeVariance.INV) return@Array argument as ConeKotlinTypeProjection

            val lowerType = if (!argument.isStarProjection() && argument.getVariance() == TypeVariance.IN) {
                argument.getType() as ConeKotlinType
            } else {
                null
            }

            ConeCapturedType(status, lowerType, argument as ConeKotlinTypeProjection)
        }

        for (index in 0 until type.argumentsCount()) {
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
        return this is ConeClassLikeSymbol && classId.asString() == "kotlin/Any"
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is ConeClassLikeSymbol && classId.asString() == "kotlin/Nothing"
    }


    override fun KotlinTypeMarker.isNotNullNothing(): Boolean {
        require(this is ConeKotlinType)
        return typeConstructor().isNothingConstructor() && !this.nullability.isNullable
    }


    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        if (isError()) return false
        if (this is ConeCapturedType) return true
        if (this is ConeTypeVariableType) return false
        require(this is ConeLookupTagBasedType)
        val typeConstructor = this.typeConstructor()
        return typeConstructor is FirClassSymbol ||
                typeConstructor is FirTypeParameterSymbol
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        TODO("not implemented")
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        return false //TODO
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        return false // TODO
    }

    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker {
        return types.first() // TODO: proper implementation
    }

    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        return types.first() // TODO: proper implementation
    }

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return when (type) {
            is ConeAbbreviatedType -> prepareType(type.directExpansionType(session) ?: ConeClassErrorType("unresolved"))
            else -> type
        }
    }
}

class ConeTypeCheckerContext(override val isErrorTypeEqualsToAnything: Boolean, override val session: FirSession) :
    AbstractTypeCheckerContext(), ConeTypeContext {
    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy.DoCustomTransform {
        require(type is ConeKotlinType)
        val declaration = when (type) {
            is ConeClassType -> type.lookupTag.toSymbol(session)?.firUnsafe<FirRegularClass>()
            else -> null
        }

        val substitutor = if (declaration != null) {
            val substitution =
                declaration.typeParameters.zip(type.typeArguments).associate { (parameter, argument) ->
                    parameter.symbol as ConeTypeParameterSymbol to ((argument as? ConeTypedProjection)?.type ?: TODO("ANY?"))
                }
            ConeSubstitutorByMap(substitution)
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

}

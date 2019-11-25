/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface ConeInferenceContext : TypeSystemInferenceExtensionContext,
    ConeTypeContext {

    val symbolProvider: FirSymbolProvider get() = session.firSymbolProvider

    override val isErrorTypeAllowed: Boolean get() = false

    override fun nullableNothingType(): SimpleTypeMarker {
        return StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nullableAnyType(): SimpleTypeMarker {
        return StandardClassIds.Any(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nothingType(): SimpleTypeMarker {
        return StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), false)
    }

    override fun anyType(): SimpleTypeMarker {
        return StandardClassIds.Any(symbolProvider).constructType(emptyArray(), false)
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound is ConeKotlinType)
        require(upperBound is ConeKotlinType)

        return ConeFlexibleType(lowerBound, upperBound)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean
    ): SimpleTypeMarker {
        require(constructor is FirClassifierSymbol<*>)
        return when (constructor) {
            is FirClassLikeSymbol<*> -> ConeClassLikeTypeImpl(
                constructor.toLookupTag(),
                (arguments as List<ConeKotlinTypeProjection>).toTypedArray(),
                nullable
            )
            is FirTypeParameterSymbol -> ConeTypeParameterTypeImpl(
                constructor.toLookupTag(),
                nullable
            )
            else -> error("!")
        }

    }

    override fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker {
        require(type is ConeKotlinType)
        return when (variance) {
            TypeVariance.INV -> type
            TypeVariance.IN -> ConeKotlinTypeProjectionIn(type)
            TypeVariance.OUT -> ConeKotlinTypeProjectionOut(type)
        }
    }

    override fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker {
        return ConeStarProjection
    }

    override fun newBaseTypeCheckerContext(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): AbstractTypeCheckerContext =
        ConeTypeCheckerContext(errorTypesEqualToAnything, stubTypesEqualToAnything, session)

    override fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean {
        require(this is ConeKotlinType)
        return this is ConeCapturedType /*|| this is ConeTypeVariable // TODO */
                || this is ConeTypeParameterType
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is ConeKotlinType)
        // if (this is TypeUtils.SpecialType) return 0 // TODO: WTF?

        val maxInArguments = this.typeArguments.asSequence().map {
            if (it.isStarProjection()) 1 else it.getType().typeDepth()
        }.max() ?: 0

        return maxInArguments + 1
    }

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean {
        return this.containsInternal(predicate)
    }

    private fun KotlinTypeMarker?.containsInternal(
        predicate: (KotlinTypeMarker) -> Boolean,
        visited: HashSet<KotlinTypeMarker> = hashSetOf()
    ): Boolean {
        if (this == null) return false
        if (this in visited) return false
        visited += this

        /*
        TODO:?
        UnwrappedType unwrappedType = type.unwrap();
         */

        if (predicate(this)) return true

        val flexibleType = this.asFlexibleType()
        if (flexibleType != null
            && (flexibleType.lowerBound().containsInternal(predicate, visited)
                    || flexibleType.upperBound().containsInternal(predicate, visited))
        ) {
            return true
        }


        if (this is DefinitelyNotNullTypeMarker
            && this.original().containsInternal(predicate, visited)
        ) {
            return true
        }
        /*
        TODO:

        TypeConstructor typeConstructor = type.getConstructor();
        if (typeConstructor instanceof IntersectionTypeConstructor) {
            IntersectionTypeConstructor intersectionTypeConstructor = (IntersectionTypeConstructor) typeConstructor;
            for (KotlinType supertype : intersectionTypeConstructor.getSupertypes()) {
                if (contains(supertype, isSpecialType, visited)) return true;
            }
            return false;
        }
         */

        repeat(argumentsCount()) { index ->
            val argument = getArgument(index)
            if (!argument.isStarProjection() && argument.getType().containsInternal(predicate, visited)) return true
        }

        return false
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        return this is FirClassLikeSymbol<*> && this.classId == StandardClassIds.Unit
    }

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        if (this.size == 1) return this.first()

        val context = newBaseTypeCheckerContext(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true)
        return this.firstOrNull { candidate ->
            this.all { other ->
                // We consider error types equal to anything here, so that intersections like
                // {Array<String>, Array<[ERROR]>} work correctly
                candidate == other || AbstractTypeChecker.equalTypes(context, candidate, other)
            }
        }
    }

    override fun KotlinTypeMarker.isUnit(): Boolean {
        require(this is ConeKotlinType)
        return this.typeConstructor().isUnitTypeConstructor() && !this.isNullable
    }

    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return this.withNullability(ConeNullability.create(nullable))
    }

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        return this.withNullability(false) //TODO("not implemented")
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        return this.withNullability(false) //TODO("not implemented")
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is ConeKotlinType?)
        require(constructorProjection is ConeKotlinTypeProjection)
        return ConeCapturedType(
            captureStatus,
            lowerType,
            constructor = ConeCapturedTypeConstructor(constructorProjection, constructorSupertypes.cast())
        )
    }

    override fun createStubType(typeVariable: TypeVariableMarker): StubTypeMarker {
        require(typeVariable is ConeTypeVariable) { "$typeVariable should subtype of ${ConeTypeVariable::class.qualifiedName}" }
        return ConeStubType(typeVariable, ConeNullability.create(typeVariable.defaultType().isMarkedNullable()))
    }

    override fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker {
        return this // TODO
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return this.withArguments(newArguments.cast<List<ConeKotlinTypeProjection>>().toTypedArray())
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean {
        return false // TODO
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean {
        return false // TODO
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeVariable)
        return this.typeConstructor
    }

    override fun KotlinTypeMarker.mayBeTypeVariable(): Boolean {
        require(this is ConeKotlinType)
        return this is ConeTypeVariableType
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        require(this is ConeCapturedType)
        return this.constructor.projection
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is ConeDefinitelyNotNullType)
        return this.original as SimpleTypeMarker
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        if (map.isEmpty()) return createEmptySubstitutor()
        return object : AbstractConeSubstitutor(),
            TypeSubstitutorMarker {
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
                val new = map[type.typeConstructor()] ?: return null
                return makeNullableIfNeed(type.isMarkedNullable, new as ConeKotlinType)
            }
        }
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return ConeSubstitutor.Empty
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        if (this === NoSubstitutor) return type
        require(this is ConeSubstitutor)
        require(type is ConeKotlinType)
        return this.substituteOrSelf(type)
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is ConeTypeVariable)
        return this.defaultType
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        return type
    }


    override fun createErrorTypeWithCustomConstructor(debugName: String, constructor: TypeConstructorMarker): KotlinTypeMarker {
        return ConeKotlinErrorType("$debugName c: $constructor")
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.captureStatus
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is ConeCapturedTypeConstructor
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.cast


interface ConeInferenceContext : TypeSystemInferenceExtensionContext, ConeTypeContext {

    val symbolProvider: FirSymbolProvider get() = session.service()

    override fun nullableNothingType(): SimpleTypeMarker {
        return StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nullableAnyType(): SimpleTypeMarker {
        return StandardClassIds.Any(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nothingType(): SimpleTypeMarker {
        return StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), false)
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound is ConeLookupTagBasedType)
        require(upperBound is ConeLookupTagBasedType)

        return ConeFlexibleType(lowerBound, upperBound)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean
    ): SimpleTypeMarker {
        require(constructor is ConeClassifierSymbol)
        when (constructor) {
            is ConeClassLikeSymbol -> return ConeClassTypeImpl(constructor.toLookupTag(), arguments.cast(), nullable)
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

    override fun newBaseTypeCheckerContext(errorTypesEqualToAnything: Boolean): AbstractTypeCheckerContext {
        return ConeTypeCheckerContext(errorTypesEqualToAnything, session)
    }

    override fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean {
        require(this is ConeKotlinType)
        return this is ConeCapturedType /*|| this is ConeTypeVariable // TODO */
                || this is ConeTypeParameterType
    }

    fun ConeKotlinType.typeDepthSimple(): Int {
        // if (this is TypeUtils.SpecialType) return 0 // TODO: WTF?

        val maxInArguments = this.typeArguments.asSequence().map {
            if (it.isStarProjection()) 1 else it.getType().typeDepth()
        }.max() ?: 0

        return maxInArguments + 1
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is ConeKotlinType)
        return this.typeDepthSimple()
    }

    override fun KotlinTypeMarker.typeDepth(): Int {
        require(this is ConeKotlinType)
        return when (this) {
            is ConeFlexibleType -> Math.max(lowerBound.typeDepthSimple(), upperBound.typeDepthSimple())
            else -> typeDepthSimple()
        }
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

        val simpleType = this.asSimpleType() ?: return false
        repeat(simpleType.argumentsCount()) { index ->
            val argument = simpleType.getArgument(index)
            if (!argument.isStarProjection() && argument.getType().containsInternal(predicate, visited)) return true
        }

        return false
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        return this is ConeClassLikeSymbol && this.classId == StandardClassIds.Unit
    }

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        if (this.size == 1) return this.first()

        val context = newBaseTypeCheckerContext(true)
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
        TODO("not implemented")
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        TODO("not implemented")
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
            captureStatus, lowerType, constructor = ConeCapturedTypeConstructor(constructorProjection, constructorSupertypes.cast())
        )
    }

    override fun createStubType(typeVariable: TypeVariableMarker): StubTypeMarker {
        TODO("not implemented")
    }

    override fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker {
        return this // TODO
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return this.withArguments(newArguments.toTypedArray().cast())
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

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        require(this is ConeCapturedType)
        return this.constructor.projection
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

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is ConeDefinitelyNotNullType)
        return this.original()
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        return NoSubstitutor
        //TODO("not implemented")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        if (this === NoSubstitutor) return type
        TODO("Not implemented")
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is ConeTypeVariable)
        return this.defaultType
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        return type
    }


}

fun ConeInferenceContext.hasNullableSuperType(type: ConeKotlinType): Boolean {
    if (type is ConeClassLikeType) return false

    if (type !is ConeLookupTagBasedType) return false // TODO?
    val symbol = type.lookupTag.toSymbol(session) ?: return false // TODO?!
    for (superType in symbol.supertypes()) {
        if (superType.isNullableType()) return true
    }
//
//    for (KotlinType supertype : getImmediateSupertypes(type)) {
//        if (isNullableType(supertype)) return true;
//    }

    return false
}

class ConeTypeVariableTypeConstructor(val debugName: String) : ConeSymbol, ConeClassifierLookupTag {
    override val name: Name get() = Name.identifier(debugName)


}

class TypeParameterBasedTypeVariable(val typeParameterSymbol: FirTypeParameterSymbol) :
    ConeTypeVariable(typeParameterSymbol.name.identifier)

open class ConeTypeVariable(name: String) : TypeVariableMarker {
    val typeConstructor = ConeTypeVariableTypeConstructor(name)
    val defaultType = ConeTypeVariableType(ConeNullability.NOT_NULL, typeConstructor)
}

class InferenceComponents(val ctx: TypeSystemInferenceExtensionContextDelegate) {
    val approximator = object : AbstractTypeApproximator(ctx) {}
    val incorporator = ConstraintIncorporator(approximator, TrivialConstraintTypeInferenceOracle(ctx))
    val injector = ConstraintInjector(incorporator, approximator)

    fun createConstraintSystem(): NewConstraintSystemImpl {
        return NewConstraintSystemImpl(injector, ctx)
    }
}


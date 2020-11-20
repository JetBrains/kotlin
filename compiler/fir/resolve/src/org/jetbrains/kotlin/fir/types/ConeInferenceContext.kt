/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.isPrimitiveNumberOrUnsignedNumberType
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.NoSubstitutor
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface ConeInferenceContext : TypeSystemInferenceExtensionContext, ConeTypeContext {

    val symbolProvider: FirSymbolProvider get() = session.firSymbolProvider

    override fun nullableNothingType(): SimpleTypeMarker {
        return session.builtinTypes.nullableNothingType.type//StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nullableAnyType(): SimpleTypeMarker {
        return session.builtinTypes.nullableAnyType.type//StandardClassIds.Any(symbolProvider).constructType(emptyArray(), true)
    }

    override fun nothingType(): SimpleTypeMarker {
        return session.builtinTypes.nothingType.type//StandardClassIds.Nothing(symbolProvider).constructType(emptyArray(), false)
    }

    override fun anyType(): SimpleTypeMarker {
        return session.builtinTypes.anyType.type//StandardClassIds.Any(symbolProvider).constructType(emptyArray(), false)
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound is ConeKotlinType)
        require(upperBound is ConeKotlinType)

        return coneFlexibleOrSimpleType(this, lowerBound, upperBound)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean
    ): SimpleTypeMarker {
        val attributes = if (isExtensionFunction) // TODO: assert correct type constructor
            ConeAttributes.create(listOf(CompilerConeAttributes.ExtensionFunctionType))
        else ConeAttributes.Empty
        @Suppress("UNCHECKED_CAST")
        return when (constructor) {
            is ConeClassLikeLookupTag -> ConeClassLikeTypeImpl(
                constructor,
                (arguments as List<ConeTypeProjection>).toTypedArray(),
                nullable,
                attributes,
            )
            is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(
                constructor,
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

    // TODO: implement checking for extension function
    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is ConeKotlinType)
        return false
    }

    override fun KotlinTypeMarker.typeDepth() = when (this) {
        is ConeSimpleKotlinType -> typeDepth()
        is ConeFlexibleType -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> error("Type should be simple or flexible: $this")
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is ConeKotlinType)
        // if (this is TypeUtils.SpecialType) return 0 // TODO: WTF?

        var maxArgumentDepth = 0
        for (arg in typeArguments) {
            val current = if (arg is ConeStarProjection) 1 else (arg as ConeKotlinTypeProjection).type.typeDepth()
            if (current > maxArgumentDepth) {
                maxArgumentDepth = current
            }
        }

        var result = maxArgumentDepth + 1

        if (this is ConeClassLikeType) {
            val fullyExpanded = fullyExpandedType(session)
            if (this !== fullyExpanded) {
                val fullyExpandedTypeDepth = fullyExpanded.typeDepth()
                if (fullyExpandedTypeDepth > result) {
                    result = fullyExpandedTypeDepth
                }
            }
        }

        return result
    }

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean {
        return this.containsInternal(predicate)
    }

    private fun KotlinTypeMarker?.containsInternal(
        predicate: (KotlinTypeMarker) -> Boolean,
        visited: HashSet<KotlinTypeMarker> = hashSetOf()
    ): Boolean {
        if (this == null) return false
        if (!visited.add(this)) return false

        /*
        TODO:?
        UnwrappedType unwrappedType = type.unwrap();
         */

        if (predicate(this)) return true

        val flexibleType = this as? ConeFlexibleType
        if (flexibleType != null
            && (flexibleType.lowerBound.containsInternal(predicate, visited)
                    || flexibleType.upperBound.containsInternal(predicate, visited))
        ) {
            return true
        }


        if (this is ConeDefinitelyNotNullType
            && this.original.containsInternal(predicate, visited)
        ) {
            return true
        }

        if (this is ConeIntersectionType) {
            return this.intersectedTypes.any { it.containsInternal(predicate, visited) }
        }

        repeat(argumentsCount()) { index ->
            val argument = getArgument(index)
            if (!argument.isStarProjection() && argument.getType().containsInternal(predicate, visited)) return true
        }

        return false
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && this.classId == StandardClassIds.Unit
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

    override fun KotlinTypeMarker.isBuiltinFunctionalTypeOrSubtype(): Boolean {
        require(this is ConeKotlinType)
        return this.isTypeOrSubtypeOf {
            (it.lowerBoundIfFlexible() as ConeKotlinType).isBuiltinFunctionalType(session)
        }
    }


    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return this.withNullability(ConeNullability.create(nullable), this@ConeInferenceContext)
    }

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return makeConeTypeDefinitelyNotNullOrNotNull()
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return makeConeTypeDefinitelyNotNullOrNotNull() as SimpleTypeMarker
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is ConeKotlinType?)
        require(constructorProjection is ConeTypeProjection)
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
        return this.withArguments(newArguments.cast<List<ConeTypeProjection>>().toTypedArray())
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean {
        require(this is ConeKotlinType)
        return attributes.exact != null
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean {
        require(this is ConeKotlinType)
        return attributes.noInfer != null
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeVariable)
        return this.typeConstructor
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        require(this is ConeCapturedType)
        return this.constructor.projection
    }

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        require(this is ConeCapturedType)
        return this.constructor.typeParameterMarker
    }

    override fun CapturedTypeMarker.withNotNullProjection(): KotlinTypeMarker {
        require(this is ConeCapturedType)
        return this // TODO
    }

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean {
        require(this is ConeCapturedType)
        return false // TODO
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is ConeDefinitelyNotNullType)
        return this.original as SimpleTypeMarker
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): ConeSubstitutor {
        if (map.isEmpty()) return createEmptySubstitutor()
        return object : AbstractConeSubstitutor(),
            TypeSubstitutorMarker {
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
                if (type !is ConeLookupTagBasedType && type !is ConeStubType) return null
                val new = map[type.typeConstructor()] ?: return null
                return (new as ConeKotlinType).approximateIntegerLiteralType().updateNullabilityIfNeeded(type)
            }
        }
    }

    override fun createEmptySubstitutor(): ConeSubstitutor {
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

    override fun KotlinTypeMarker.isSpecial(): Boolean {
        // TODO
        return false
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is ConeTypeVariableTypeConstructor
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        return false
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        return type
    }

    override fun createErrorType(debugName: String): ConeClassErrorType {
        return ConeClassErrorType(ConeIntermediateDiagnostic(debugName))
    }

    override fun createErrorTypeWithCustomConstructor(debugName: String, constructor: TypeConstructorMarker): KotlinTypeMarker {
        return ConeKotlinErrorType(ConeIntermediateDiagnostic("$debugName c: $constructor"))
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.captureStatus
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is ConeCapturedTypeConstructor
    }

    override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
        // TODO
        return this
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        if (this is ErrorTypeConstructor) return createErrorType(reason)
        if (this is ConeClassLikeLookupTag) return createErrorType("Not found classifier: $classId")
        return createErrorType("Unknown reason")
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        return ConeIntegerLiteralTypeImpl.findCommonSuperType(explicitSupertypes)
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
        require(this is ConeIntegerLiteralType)
        return this.getApproximatedType()
    }

    override fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        require(this is ConeKotlinType)
        if (this !is ConeClassLikeType) return false
        return isPrimitiveNumberOrUnsignedNumberType()
    }

    override fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean {
        require(this is ConeKotlinType)
        return this.isBuiltinFunctionalType(session)
    }

    private fun ConeKotlinType.isTypeOrSubtypeOf(predicate: (ConeKotlinType) -> Boolean): Boolean {
        return predicate(this) || DFS.dfsFromNode(
            this,
            {
                // FIXME supertypes of type constructor contain unsubstituted arguments
                @Suppress("UNCHECKED_CAST")
                it.typeConstructor().supertypes() as Collection<ConeKotlinType>
            },
            DFS.VisitedWithSet(),
            object : DFS.AbstractNodeHandler<ConeKotlinType, Boolean>() {
                private var result = false

                override fun beforeChildren(current: ConeKotlinType): Boolean {
                    if (predicate(current)) {
                        result = true
                    }
                    return !result
                }

                override fun result() = result
            }
        )
    }

    override fun KotlinTypeMarker.isSuspendFunctionTypeOrSubtype(): Boolean {
        require(this is ConeKotlinType)
        return isTypeOrSubtypeOf {
            (it.lowerBoundIfFlexible() as ConeKotlinType).isSuspendFunctionType(session)
        }
    }

    override fun KotlinTypeMarker.isExtensionFunctionType(): Boolean {
        require(this is ConeKotlinType)
        return this.lowerBoundIfFlexible().safeAs<ConeKotlinType>()?.isExtensionFunctionType(session) == true
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun KotlinTypeMarker.extractArgumentsForFunctionalTypeOrSubtype(): List<KotlinTypeMarker> {
        val builtInFunctionalType = getFunctionalTypeFromSupertypes().cast<ConeKotlinType>()
        return buildList {
            // excluding return type
            for (index in 0 until builtInFunctionalType.argumentsCount() - 1) {
                add(builtInFunctionalType.getArgument(index).getType())
            }
        }
    }

    override fun KotlinTypeMarker.getFunctionalTypeFromSupertypes(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        assert(this.isBuiltinFunctionalTypeOrSubtype()) {
            "Not a function type or subtype: ${this.render()}"
        }

        return fullyExpandedType(session).let {
            val simpleType = it.lowerBoundIfFlexible()
            if ((simpleType as ConeKotlinType).isBuiltinFunctionalType(session))
                this
            else {
                var functionalSupertype: KotlinTypeMarker? = null
                simpleType.anySuperTypeConstructor { typeConstructor ->
                    simpleType.fastCorrespondingSupertypes(typeConstructor)?.any { superType ->
                        val isFunctional = superType.cast<ConeKotlinType>().isBuiltinFunctionalType(session)
                        if (isFunctional)
                            functionalSupertype = superType
                        isFunctional
                    } ?: false
                }
                functionalSupertype ?: error("Failed to find functional supertype for $simpleType")
            }
        }
    }

    override fun getFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker {
        val classId = if (isSuspend)
            StandardNames.getSuspendFunctionClassId(parametersNumber)
        else StandardNames.getFunctionClassId(parametersNumber)
        return session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.toLookupTag()
            ?: error("Can't find Function type")
    }

    override fun getKFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker {
        val classId = if (isSuspend)
            StandardNames.getKSuspendFunctionClassId(parametersNumber)
        else StandardNames.getKFunctionClassId(parametersNumber)
        return session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.toLookupTag()
            ?: error("Can't find KFunction type")
    }
}

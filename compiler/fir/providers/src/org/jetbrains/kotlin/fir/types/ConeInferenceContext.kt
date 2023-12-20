/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.isPrimitiveNumberOrUnsignedNumberType
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

interface ConeInferenceContext : TypeSystemInferenceExtensionContext, ConeTypeContext {

    val symbolProvider: FirSymbolProvider get() = session.symbolProvider

    override fun nullableNothingType(): ConeClassLikeType {
        return session.builtinTypes.nullableNothingType.type
    }

    override fun nullableAnyType(): ConeClassLikeType {
        return session.builtinTypes.nullableAnyType.type
    }

    override fun nothingType(): ConeClassLikeType {
        return session.builtinTypes.nothingType.type
    }

    override fun anyType(): ConeClassLikeType {
        return session.builtinTypes.anyType.type
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
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        val attributesList = attributes?.filterIsInstanceTo<ConeAttribute<*>, MutableList<ConeAttribute<*>>>(mutableListOf())
        val coneAttributes: ConeAttributes = if (isExtensionFunction) {
            require(constructor is ConeClassLikeLookupTag)
            // We don't want to create new instance of ConeAttributes which
            //   contains only CompilerConeAttributes.ExtensionFunctionType
            //   to avoid memory consumption
            if (attributesList != null) {
                attributesList += CompilerConeAttributes.ExtensionFunctionType
                ConeAttributes.create(attributesList)
            } else {
                ConeAttributes.WithExtensionFunctionType
            }
        } else {
            attributesList?.let { ConeAttributes.create(it) } ?: ConeAttributes.Empty
        }
        @Suppress("UNCHECKED_CAST")
        return when (constructor) {
            is ConeClassLikeLookupTag -> ConeClassLikeTypeImpl(
                constructor,
                (arguments as List<ConeTypeProjection>).toTypedArray(),
                nullable,
                coneAttributes,
            )
            is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(
                constructor,
                nullable,
                coneAttributes
            )
            is ConeIntersectionType -> if (coneAttributes === constructor.attributes) {
                constructor
            } else {
                ConeIntersectionType(
                    constructor.intersectedTypes.map { it.withAttributes(coneAttributes) },
                    constructor.alternativeType?.withAttributes(coneAttributes)
                )
            }
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

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState = TypeCheckerState(
        errorTypesEqualToAnything,
        stubTypesEqualToAnything,
        allowedTypeVariable = true,
        typeSystemContext = this,
        kotlinTypePreparator = ConeTypePreparator(session),
        kotlinTypeRefiner = AbstractTypeRefiner.Default
    )

    override fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean {
        require(this is ConeKotlinType)
        return this is ConeCapturedType || this is ConeTypeVariableType
                || this is ConeTypeParameterType
    }

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is ConeKotlinType)
        return this.isExtensionFunctionType
    }

    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        require(this is ConeStubType)
        return this.constructor.variable.typeConstructor
    }

    override fun KotlinTypeMarker.typeDepth() = when (this) {
        is ConeSimpleKotlinType -> typeDepth()
        is ConeFlexibleType -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> errorWithAttachment("Type should be simple or flexible: ${this::class.java}") {
            withConeTypeEntry("type", this@typeDepth as? ConeKotlinType)
        }
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is ConeKotlinType)
        // if (this is TypeUtils.SpecialType) return 0 // TODO: WTF?

        if (this is ConeClassLikeType) {
            val fullyExpanded = fullyExpandedType(session)
            if (this !== fullyExpanded) {
                return fullyExpanded.typeDepth()
            }
        }

        var maxArgumentDepth = 0
        for (arg in typeArguments) {
            val current = if (arg is ConeStarProjection) 1 else (arg as ConeKotlinTypeProjection).type.typeDepth()
            if (current > maxArgumentDepth) {
                maxArgumentDepth = current
            }
        }

        return maxArgumentDepth + 1
    }

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean {
        return this.containsInternal(predicate)
    }

    private fun KotlinTypeMarker?.containsInternal(
        predicate: (KotlinTypeMarker) -> Boolean,
    ): Boolean {
        if (this == null) return false

        if (predicate(this)) return true

        val flexibleType = this as? ConeFlexibleType
        if (flexibleType != null
            && (flexibleType.lowerBound.containsInternal(predicate) || flexibleType.upperBound.containsInternal(predicate))
        ) {
            return true
        }


        if (this is ConeDefinitelyNotNullType && this.original.containsInternal(predicate)) {
            return true
        }

        if (this is ConeIntersectionType) {
            return this.intersectedTypes.any { it.containsInternal(predicate) }
        }

        if (this is ConeCapturedType) {
            return this.constructor.projection.type?.containsInternal(predicate) == true
        }

        repeat(argumentsCount()) { index ->
            val argument = getArgument(index)
            if (!argument.isStarProjection() && argument.getType().containsInternal(predicate)) return true
        }

        return false
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && this.classId == StandardClassIds.Unit
    }

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        if (this.size == 1) return this.first()

        val context = newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true)
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

    override fun KotlinTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean {
        require(this is ConeKotlinType)
        return this.isTypeOrSubtypeOf {
            (it.lowerBoundIfFlexible() as ConeKotlinType).isSomeFunctionType(session)
        }
    }


    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return this.withNullability(ConeNullability.create(nullable), this@ConeInferenceContext)
    }

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return makeConeTypeDefinitelyNotNullOrNotNull(this@ConeInferenceContext)
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return makeConeTypeDefinitelyNotNullOrNotNull(this@ConeInferenceContext) as SimpleTypeMarker
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is ConeKotlinType?)
        require(constructorProjection is ConeTypeProjection)
        @Suppress("UNCHECKED_CAST")
        return ConeCapturedType(
            captureStatus,
            lowerType,
            constructor = ConeCapturedTypeConstructor(constructorProjection, constructorSupertypes as List<ConeKotlinType>)
        )
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        require(typeVariable is ConeTypeVariable) { "$typeVariable should subtype of ${ConeTypeVariable::class.qualifiedName}" }
        return ConeStubTypeForChainInference(typeVariable, ConeNullability.create(typeVariable.defaultType().isMarkedNullable()))
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        require(typeVariable is ConeTypeVariable) { "$typeVariable should subtype of ${ConeTypeVariable::class.qualifiedName}" }
        return ConeStubTypeForTypeVariableInSubtyping(typeVariable, ConeNullability.create(typeVariable.defaultType().isMarkedNullable()))
    }

    override fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return withAttributes(ConeAttributes.Empty)
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is ConeKotlinType)
        @Suppress("UNCHECKED_CAST")
        return this.withArguments((newArguments as List<ConeTypeProjection>).toTypedArray())
    }

    override fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        require(this is ConeKotlinType)
        return this.withArguments { replacement(it) as ConeTypeProjection }
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean {
        require(this is ConeKotlinType)
        return attributes.exact != null
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean {
        require(this is ConeKotlinType)
        return attributes.noInfer != null
    }

    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
        val symbol = toClassLikeSymbol() ?: return false
        if (symbol is FirAnonymousObjectSymbol) return true
        val classSymbol = symbol as? FirRegularClassSymbol ?: return false
        return classSymbol.modality == Modality.FINAL
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
        return ConeCapturedType(captureStatus, lowerType, nullability, constructor, attributes, isProjectionNotNull = true)
    }

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean {
        require(this is ConeCapturedType)
        return isProjectionNotNull
    }

    override fun CapturedTypeMarker.hasRawSuperType(): Boolean {
        require(this is ConeCapturedType)
        return constructor.supertypes?.any { superType ->
            superType.contains { it is ConeKotlinType && it.isRaw() }
        } == true
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is ConeDefinitelyNotNullType)
        return this.original
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): ConeSubstitutor {
        @Suppress("UNCHECKED_CAST")
        return createTypeSubstitutorByTypeConstructor(
            map as Map<TypeConstructorMarker, ConeKotlinType>, this, approximateIntegerLiterals = false
        )
    }

    override fun createEmptySubstitutor(): ConeSubstitutor {
        return ConeSubstitutor.Empty
    }

    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        return object : AbstractConeSubstitutor(this) {
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
                return ((type as? ConeStubTypeForTypeVariableInSubtyping)
                    ?.constructor?.variable?.defaultType)?.withNullability(type.nullability, this@ConeInferenceContext)
            }
        }
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
        // Cone type system doesn't have special types
        return false
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is ConeTypeVariableTypeConstructor
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        require(this is ConeTypeVariableTypeConstructor)
        return isContainedInInvariantOrContravariantPositions
    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): ConeErrorType {
        return ConeErrorType(ConeIntermediateDiagnostic(debugName), delegatedType = delegatedType as ConeKotlinType?)
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker {
        return ConeErrorType(ConeIntermediateDiagnostic("Uninferred type c: $constructor"))
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.captureStatus
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = false

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is ConeCapturedTypeConstructor
    }

    override fun KotlinTypeMarker.eraseContainingTypeParameters(): KotlinTypeMarker {
        val typeParameterErasureMap = this.extractTypeParameters()
            .map { (it as ConeTypeParameterLookupTag).typeParameterSymbol }
            .eraseToUpperBoundsAssociated(session)
        val substitutor by lazy { ConeSubstitutorByMap(typeParameterErasureMap, session) }
        val typeWithErasedTypeParameters = if (argumentsCount() != 0) {
            replaceArgumentsDeeply {
                val type = it.getType()
                val typeParameter =
                    (type.typeConstructor().getTypeParameterClassifier() as? ConeTypeParameterLookupTag)?.typeParameterSymbol
                if (typeParameter != null) {
                    createTypeArgument(substitutor.safeSubstitute(type), TypeVariance.OUT)
                } else it
            }
        } else if (typeConstructor().isTypeParameterTypeConstructor()) {
            substitutor.safeSubstitute(this)
        } else this

        return typeWithErasedTypeParameters
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        return this.getTypeParameterClassifier() != null
    }

    override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return withAttributes(attributes.remove(CompilerConeAttributes.Exact))
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        if (this is ErrorTypeConstructor) return createErrorType(reason, delegatedType = null)
        if (this is ConeClassLikeLookupTag) return createErrorType("Not found classifier: $classId", delegatedType = null)
        return createErrorType("Unknown reason", delegatedType = null)
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        return ConeIntegerLiteralType.findCommonSuperType(explicitSupertypes)
    }

    override fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker> {
        @Suppress("UNCHECKED_CAST")
        return (types as List<ConeKotlinType>).map { it.attributes }.reduce { x, y -> x.union(y) }.toList()
    }

    private fun AnnotationMarker.isCustomAttribute(): Boolean {
        val compilerAttributes = CompilerConeAttributes.classIdByCompilerAttributeKey
        return (this as? ConeAttribute<*>)?.key !in compilerAttributes && this !is CustomAnnotationTypeAttribute
    }

    override fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker {
        require(this is ConeKotlinType)
        @Suppress("UNCHECKED_CAST")
        val newCustomAttributes = (newAttributes as List<ConeAttribute<*>>).filter { it.isCustomAttribute() }
        val attributesToKeep = this.attributes.filterNot { it.isCustomAttribute() }
        return withAttributes(ConeAttributes.create(newCustomAttributes + attributesToKeep))
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
        require(this is ConeIntegerLiteralType)
        return this.getApproximatedType()
    }

    override fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        require(this is ConeKotlinType)
        if (this is ConeIntegerLiteralType) return true
        if (this !is ConeClassLikeType) return false
        return isPrimitiveNumberOrUnsignedNumberType()
    }

    override fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean {
        require(this is ConeKotlinType)
        return this.isSomeFunctionType(session)
    }

    fun ConeKotlinType.isTypeOrSubtypeOf(predicate: (ConeKotlinType) -> Boolean): Boolean {
        return predicate(this) || DFS.dfsFromNode(
            this,
            {
                // FIXME supertypes of type constructor contain unsubstituted arguments
                it.typeConstructor().supertypes()
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

    override fun KotlinTypeMarker.isExtensionFunctionType(): Boolean {
        require(this is ConeKotlinType)
        return (this.lowerBoundIfFlexible() as? ConeKotlinType)?.isExtensionFunctionType(session) == true
    }

    override fun KotlinTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<KotlinTypeMarker> {
        val builtInFunctionType = getFunctionTypeFromSupertypes() as ConeKotlinType
        return buildList {
            // excluding return type
            for (index in 0 until builtInFunctionType.argumentsCount() - 1) {
                add(builtInFunctionType.getArgument(index).getType())
            }
        }
    }

    override fun KotlinTypeMarker.getFunctionTypeFromSupertypes(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        assert(this.isBuiltinFunctionTypeOrSubtype()) {
            "Not a function type or subtype: ${this.renderForDebugging()}"
        }

        return fullyExpandedType(session).let {
            val simpleType = it.lowerBoundIfFlexible()
            if ((simpleType as ConeKotlinType).isSomeFunctionType(session))
                this
            else {
                var functionalSupertype: KotlinTypeMarker? = null
                simpleType.anySuperTypeConstructor { type ->
                    simpleType.fastCorrespondingSupertypes(type.typeConstructor())?.any { superType ->
                        val isFunction = (superType as ConeKotlinType).isSomeFunctionType(session)
                        if (isFunction) {
                            functionalSupertype = superType
                        }
                        isFunction
                    } ?: false
                }
                functionalSupertype
                    ?: errorWithAttachment("Failed to find functional supertype for ${simpleType::class.java}") {
                    withConeTypeEntry("type", simpleType)
                }
            }
        }
    }

    override fun KotlinTypeMarker.functionTypeKind(): FunctionTypeKind? {
        require(this is ConeKotlinType)
        return this.functionTypeKind(session)
    }

    override fun getNonReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker {
        return kind.nonReflectKind().numberedClassId(parametersNumber).toLookupTag()
    }

    override fun getReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker {
        return kind.reflectKind().numberedClassId(parametersNumber).toLookupTag()
    }

    override fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker
    ): KotlinTypeMarker {
        require(firstCandidate is ConeKotlinType)
        require(secondCandidate is ConeKotlinType)
        val intersectionType = firstCandidate.lowerBoundIfFlexible() as? ConeIntersectionType ?: error {
            "Expected type is intersection, found $firstCandidate"
        }
        return intersectionType.withAlternative(secondCandidate)
    }

    override fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean = true

    override fun KotlinTypeMarker.convertToNonRaw(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        return this.convertToNonRawVersion()
    }

    override fun createSubstitutorForSuperTypes(baseType: KotlinTypeMarker): TypeSubstitutorMarker? =
        if (baseType is ConeLookupTagBasedType) createSubstitutionForSupertype(baseType, session) else null

    override val isK2: Boolean
        get() = true
}

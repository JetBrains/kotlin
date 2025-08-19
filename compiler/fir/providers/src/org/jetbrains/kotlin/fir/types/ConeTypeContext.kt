/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirLookupTagEntry
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeCheckerState.SupertypesPolicy.DoCustomTransform
import org.jetbrains.kotlin.types.TypeCheckerState.SupertypesPolicy.LowerIfFlexible
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

interface ConeTypeContext : TypeSystemContext, TypeSystemOptimizationContext, TypeCheckerProviderContext, TypeSystemCommonBackendContext {
    val session: FirSession

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        return this is ConeIntegerLiteralType
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        return this is ConeIntegerLiteralConstantType
    }

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean {
        return this is ConeIntegerConstantOperatorType
    }

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        if (this !is ConeClassLikeLookupTag) return false
        return isLocalClass()
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        if (this !is ConeClassLikeLookupTag) return false
        return isAnonymousClass()
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() {
            require(this is ConeTypeVariableTypeConstructor)
            return this.originalTypeParameter
        }

    override fun RigidTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        return (this as? ConeIntegerLiteralType)?.possibleTypes ?: emptyList()
    }

    override fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<ConeClassLikeType>? {
        require(this is ConeKotlinType)
        return session.correspondingSupertypesCache.getCorrespondingSupertypes(this, constructor)
    }

    override fun RigidTypeMarker.isIntegerLiteralType(): Boolean {
        return this is ConeIntegerLiteralType
    }

    override fun KotlinTypeMarker.asRigidType(): RigidTypeMarker? {
        assert(this is ConeKotlinType)
        return when (this) {
            is ConeClassLikeType -> fullyExpandedType(session)
            is ConeRigidType -> this
            is ConeFlexibleType -> null
            else -> errorWithAttachment("Unknown simpleType: ${this::class}") {
                withConeTypeEntry("type", this@asRigidType as? ConeKotlinType)
            }
        }
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        return this as? ConeFlexibleType
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        return this is ConeErrorType || this is ConeErrorType || this.typeConstructor().isError() ||
                (this is ConeClassLikeType && this.lookupTag is ConeClassLikeErrorLookupTag)
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean {
        return this is ConeErrorType && this.isUninferredParameter
    }

    override fun FlexibleTypeMarker.asDynamicType(): ConeDynamicType? {
        return this as? ConeDynamicType
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        require(this is ConeKotlinType)
        return this.isRaw()
    }

    override fun FlexibleTypeMarker.upperBound(): RigidTypeMarker {
        require(this is ConeFlexibleType)
        return this.upperBound
    }

    override fun FlexibleTypeMarker.lowerBound(): RigidTypeMarker {
        require(this is ConeFlexibleType)
        return this.lowerBound
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        return this as? ConeCapturedType
    }

    override fun RigidTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        return this as? ConeDefinitelyNotNullType
    }

    override fun KotlinTypeMarker.isMarkedNullable(): Boolean {
        require(this is ConeKotlinType)
        return fullyExpandedType(session).isMarkedNullable
    }

    override fun RigidTypeMarker.withNullability(nullable: Boolean): RigidTypeMarker {
        require(this is ConeKotlinType)
        return fullyExpandedType(session).withNullability(nullable, session.typeContext) as RigidTypeMarker
    }

    override fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is ConeRigidType)
        return this.getConstructor()
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is ConeCapturedType)
        return this.constructor
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is ConeCapturedType)
        return this.captureStatus
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = false

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
        return this.typeArguments.getOrNull(index) ?: ConeStarProjection
    }

    override fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> {
        require(this is ConeKotlinType)
        return this.typeArguments.toList()
    }

    override fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is ConeKotlinType)
        return this
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
        require(this is ConeCapturedType)
        if (!this.isMarkedNullable) return this.lowerType
        return this.lowerType?.makeNullable()
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
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

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker? {
        require(this is ConeTypeProjection)
        return this.type
    }

    override fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker {
        require(this is ConeKotlinTypeProjection)
        require(newType is ConeKotlinType)
        return when (this) {
            is ConeKotlinType -> newType
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(newType)
        }
    }

    override fun TypeConstructorMarker.parametersCount(): Int {
        require(this is ConeTypeConstructorMarker)
        return when (this) {
            is ConeCapturedTypeConstructor,
            is ConeTypeVariableTypeConstructor,
            is ConeIntersectionType,
                -> 0
            is ConeClassifierLookupTag -> {
                when (val symbol = toSymbol(session)) {
                    is FirAnonymousObjectSymbol -> symbol.fir.typeParameters.size
                    is FirRegularClassSymbol -> symbol.fir.typeParameters.size
                    is FirTypeAliasSymbol -> symbol.fir.typeParameters.size
                    is FirTypeParameterSymbol, null -> 0
                }
            }
            is ConeIntegerLiteralType -> 0
            is ConeStubTypeConstructor -> 0
        }
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        return when (val symbol = toClassLikeSymbol()) {
            is FirAnonymousObjectSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            is FirRegularClassSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            is FirTypeAliasSymbol -> symbol.fir.typeParameters[index].symbol.toLookupTag()
            else -> errorWithAttachment("Unexpected FirClassLikeSymbol $symbol for ${this::class}") {
                withFirLookupTagEntry("lookupTag", this@getParameter as? ConeClassLikeLookupTag)
            }
        }
    }

    override fun TypeConstructorMarker.getParameters(): List<ConeTypeParameterLookupTag> {
        return when (val symbol = toClassLikeSymbol()) {
            is FirAnonymousObjectSymbol -> symbol.fir.typeParameters.map { it.symbol.toLookupTag() }
            is FirRegularClassSymbol -> symbol.fir.typeParameters.map { it.symbol.toLookupTag() }
            is FirTypeAliasSymbol -> symbol.fir.typeParameters.map { it.symbol.toLookupTag() }
            else -> emptyList()
        }
    }

    fun TypeConstructorMarker.toClassLikeSymbol(): FirClassLikeSymbol<*>? = (this as? ConeClassLikeLookupTag)?.toSymbol(session)

    override fun TypeConstructorMarker.supertypes(): Collection<ConeKotlinType> {
        require(this is ConeTypeConstructorMarker)
        return when (this) {
            is ConeStubTypeConstructor -> listOf(session.builtinTypes.nullableAnyType.coneType)
            is ConeTypeVariableTypeConstructor -> emptyList()
            is ConeClassifierLookupTag -> {
                when (val symbol = toSymbol(session).also { it?.lazyResolveToPhase(FirResolvePhase.TYPES) }) {
                    is FirTypeParameterSymbol -> symbol.resolvedBounds.map { it.coneType }
                    is FirClassSymbol<*> -> symbol.fir.superConeTypes
                    is FirTypeAliasSymbol -> listOfNotNull(symbol.fir.expandedConeType)
                    null -> listOf(session.builtinTypes.anyType.coneType)
                }
            }
            is ConeCapturedTypeConstructor -> supertypes.orEmpty()
            is ConeIntersectionType -> intersectedTypes
            is ConeIntegerLiteralType -> supertypes
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return this is ConeIntersectionType
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        // See KT-55383
        return this is ConeClassLikeLookupTag || this is ConeStubTypeConstructor
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        return ((this as? ConeClassLikeLookupTag)?.toClassLikeSymbol()?.fir as? FirClass)?.classKind == ClassKind.INTERFACE
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
        return this.bounds()[index].coneType
    }

    override fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker> {
        require(this is ConeTypeParameterLookupTag)
        return this.bounds().map { it.coneType }
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is ConeTypeParameterLookupTag)
        return this
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is ConeTypeParameterLookupTag)
        this.typeParameterSymbol.lazyResolveToPhase(FirResolvePhase.TYPES)
        return this.bounds().any { typeRef ->
            typeRef.coneType.contains { it.typeConstructor() == this.getTypeConstructor() }
                    && (selfConstructor == null || typeRef.coneType.typeConstructor() == selfConstructor)
        }
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        return c1 == c2
    }

    override fun TypeConstructorMarker.isDenotable(): Boolean {
        require(this is ConeTypeConstructorMarker)
        return when (this) {
            is ConeClassifierLookupTag -> this !is ConeClassLikeErrorLookupTag

            is ConeStubTypeConstructor,
            is ConeCapturedTypeConstructor,
            is ConeTypeVariableTypeConstructor,
            is ConeIntegerLiteralType,
            is ConeIntersectionType,
                -> false
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

    override fun captureFromExpression(type: KotlinTypeMarker): ConeKotlinType? {
        require(type is ConeKotlinType)
        return captureFromExpressionInternal(type)
    }

    override fun captureFromArguments(type: RigidTypeMarker, status: CaptureStatus): RigidTypeMarker? {
        require(type is ConeRigidType)
        return captureFromArgumentsInternal(type, status) as RigidTypeMarker?
    }

    override fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is ConeKotlinType)
        return this
    }

    override fun identicalArguments(a: RigidTypeMarker, b: RigidTypeMarker): Boolean {
        require(a is ConeRigidType)
        require(b is ConeRigidType)
        return a.typeArguments === b.typeArguments
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StandardClassIds.Any
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StandardClassIds.Nothing
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        return this is ConeClassLikeLookupTag && classId == StandardClassIds.Array
    }

    override fun RigidTypeMarker.isSingleClassifierType(): Boolean {
        if (isError()) return false
        require(this is ConeRigidType)
        return when (this) {
            is ConeLookupTagBasedType -> {
                val typeConstructor = this.typeConstructor()
                typeConstructor is ConeClassifierLookupTag
            }
            is ConeCapturedType -> true
            is ConeTypeVariableType -> false
            is ConeIntersectionType -> false
            is ConeIntegerLiteralType -> true
            is ConeStubType -> true
            is ConeDefinitelyNotNullType -> true
            is ConeErrorUnionType -> isTypeParameter()
        }
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        if (this is ConeClassLikeType) {
            return isPrimitive
        }
        return false
    }

    override fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is ConeKotlinType)
        return attributes.toList()
    }

    override fun RigidTypeMarker.isStubType(): Boolean {
        return this is ConeStubType
    }

    override fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean {
        return this is ConeStubTypeForTypeVariableInSubtyping
    }

    override fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker {
        if (this !is ConeStubTypeConstructor) return this
        if (this.isTypeVariableInSubtyping) return this
        if (this.isForFixation) return this
        return this.variable.typeConstructor
    }

    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return ConeTypeIntersector.intersectTypes(
            this as ConeInferenceContext, types as Collection<ConeSimpleKotlinType>
        ) as SimpleTypeMarker
    }

    override fun intersectTypes(types: Collection<KotlinTypeMarker>): ConeKotlinType {
        @Suppress("UNCHECKED_CAST")
        return ConeTypeIntersector.intersectTypes(this as ConeInferenceContext, types as Collection<ConeKotlinType>)
    }

    override fun KotlinTypeMarker.isNullableType(): Boolean {
        require(this is ConeKotlinType)
        return canBeNull(session)
    }

    private fun TypeConstructorMarker.toFirRegularClass(): FirRegularClass? {
        return toClassLikeSymbol()?.fir as? FirRegularClass
    }

    override fun nullableAnyType(): ConeClassLikeType = session.builtinTypes.nullableAnyType.coneType as ConeClassLikeType

    override fun arrayType(componentType: KotlinTypeMarker): ConeClassLikeType {
        require(componentType is ConeKotlinType)
        return componentType.createArrayType(nullable = false)
    }

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean {
        require(this is ConeKotlinType)
        return this.classId == StandardClassIds.Array
    }

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        val firRegularClass = toFirRegularClass() ?: return false

        // NB: This API is used to determine if a given type [isMostPreciseCovariantArgument] (at `typeMappingUtil.kt`),
        // affecting the upper bound wildcard when mapping the enclosing type to [PsiType]. See KT-57578 for more details.
        // The counterpart in K1, [ClassicTypeSystemContext], uses [ClassDescriptor.isFinalClass] in `ModalityUtils.kt`,
        // which filters out `enum` class. It seems [ClassDescriptor.isFinalOrEnum] is for truly `final` class.
        // That is, the overall API name---isFinalClassOr...---is misleading.
        val classKind = firRegularClass.classKind
        return classKind.isEnumEntry ||
                classKind.isAnnotationClass ||
                classKind.isObject ||
                classKind.isClass && firRegularClass.symbol.modality == Modality.FINAL
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        require(this is ConeKotlinType)
        val compilerAttribute = CompilerConeAttributes.compilerAttributeKeyByFqName[fqName]
        if (compilerAttribute != null) {
            return compilerAttribute in attributes
        }
        if (fqName == ParameterNameTypeAttribute.ANNOTATION_CLASS_ID.asSingleFqName()) {
            return ParameterNameTypeAttribute.KEY in attributes
        }
        return customAnnotations.any {
            it.resolvedType.fullyExpandedType(session).classId?.asSingleFqName() == fqName
        }
    }

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
        require(this is ConeKotlinType)
        // We don't check for compiler attributes because all of them doesn't have parameters
        val annotationCall = customAnnotations.firstOrNull {
            it.resolvedType.fullyExpandedType(session).classId?.asSingleFqName() == fqName
        } ?: return null

        if (annotationCall is FirAnnotationCall) {
            annotationCall.containingDeclarationSymbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
        }

        val argument = when (val argument = annotationCall.argumentMapping.mapping.values.firstOrNull() ?: return null) {
            is FirVarargArgumentsExpression -> argument.arguments.firstOrNull()
            is FirArrayLiteral -> argument.arguments.firstOrNull()
            is FirNamedArgumentExpression -> argument.expression
            else -> argument
        } ?: return null
        return (argument as? FirLiteralExpression)?.value
    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        return this as? ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.isInlineClass(): Boolean {
        val fields = getValueClassProperties() ?: return false
        return this@ConeTypeContext.valueClassLoweringKind(fields) == ValueClassKind.Inline
    }

    override fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean {
        val fields = getValueClassProperties() ?: return false
        return this@ConeTypeContext.valueClassLoweringKind(fields) == ValueClassKind.MultiField
    }

    override fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, RigidTypeMarker>>? {
        val firClass = toClassLikeSymbol()?.fullyExpandedClass(session)?.fir ?: return null
        if (!firClass.isInlineOrValue) return null
        return firClass.primaryConstructorIfAny(session)
            ?.valueParameterSymbols
            ?.map { it.name to it.resolvedReturnType as RigidTypeMarker }
            ?.takeIf { it.isNotEmpty() }
    }

    override fun TypeConstructorMarker.isInnerClass(): Boolean {
        return toFirRegularClass()?.isInner == true
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker {
        require(this is ConeTypeParameterLookupTag)
        return this.bounds().getOrNull(0)?.coneType
            ?: session.builtinTypes.nullableAnyType.coneType
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ConeTypeParameterLookupTag.bounds(): List<FirTypeRef> = symbol.resolvedBounds

    override fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker? {
        require(this is ConeKotlinType)
        return unsubstitutedUnderlyingTypeForInlineClass(session)
    }

    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? =
        getClassFqNameUnsafe()?.let(StandardNames.FqNames.fqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? =
        getClassFqNameUnsafe()?.let(StandardNames.FqNames.arrayClassFqNameToPrimitiveType::get)

    override fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean =
        getClassFqNameUnsafe()?.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE.shortName()) == true

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? {
        if (this !is ConeClassLikeLookupTag) return null
        return classId.asSingleFqName().toUnsafe()
    }

    override fun TypeParameterMarker.getName(): Name = (this as ConeTypeParameterLookupTag).name

    override fun TypeParameterMarker.isReified(): Boolean {
        require(this is ConeTypeParameterLookupTag)
        return typeParameterSymbol.fir.isReified
    }

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val classKind = typeConstructor().toFirRegularClass()?.classKind ?: return false
        return classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.INTERFACE
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        return false
    }

    override fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy {
        if (type.argumentsCount() == 0) return LowerIfFlexible
        require(type is ConeKotlinType)
        val declaration = when (type) {
            is ConeClassLikeType -> type.lookupTag.toSymbol(session)?.fir
            else -> null
        }

        val substitutor = if (declaration is FirTypeParameterRefsOwner) {
            val substitution =
                declaration.typeParameters.zip(type.typeArguments).associate { (parameter, argument) ->
                    parameter.symbol to ((argument as? ConeKotlinTypeProjection)?.type
                        ?: session.builtinTypes.nullableAnyType.coneType)//StandardClassIds.Any(session.firSymbolProvider).constructType(emptyArray(), isNullable = true))
                }
            substitutorByMap(substitution, session)
        } else {
            ConeSubstitutor.Empty
        }
        return object : DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): RigidTypeMarker {
                val lowerBound = type.lowerBoundIfFlexible()
                require(lowerBound is ConeRigidType)
                return substitutor.substituteOrSelf(lowerBound) as RigidTypeMarker
            }

        }
    }

    override fun KotlinTypeMarker.isTypeVariableType(): Boolean {
        return this is ConeTypeVariableType
    }

    override fun RigidTypeMarker.isErrorUnion(): Boolean {
        return this is ConeErrorUnionType
    }

    override fun RigidTypeMarker.isValueType(): Boolean {
        return this is ConeValueType
    }

    override fun RigidTypeMarker.valueType(): ValueTypeMarker {
        this as ConeRigidType
        return when (this) {
            is ConeErrorUnionType -> valueType
            is ConeValueType -> this
        }
    }

    override fun RigidTypeMarker.errorType(): ErrorTypeMarker {
        this as ConeRigidType
        return when (this) {
            is ConeErrorUnionType -> errorType
            is ConeValueType -> CEBotType
        }
    }

    override fun KotlinTypeMarker.projectOnValue(): KotlinTypeMarker {
        return when (this) {
            is ConeDynamicType -> this
            is ConeFlexibleType -> {
                val lb = lowerBound.projectOnValue() as ConeRigidType
                val ub = upperBound.projectOnValue() as ConeRigidType
                ConeFlexibleType(lb, ub, isTrivial)
            }
            is ConeErrorUnionType -> valueType
            else -> this
        }
    }

    override fun KotlinTypeMarker.projectOnError(): KotlinTypeMarker {
        return when (this) {
            is ConeFlexibleType -> {
                val lb = lowerBound.projectOnError() as? ConeErrorUnionType
                val ub = upperBound.projectOnError() as? ConeErrorUnionType
                if (lb == ub) {
                    return lb ?: StandardTypes.Nothing
                } else {
                    error("Flexible type with different error components detected: $this")
                }
            }
            is ConeErrorUnionType -> {
                if (valueType.isNothing) {
                    this
                } else {
                    ConeErrorUnionType.create(StandardTypes.Nothing, errorType)
                }
            }
            else -> StandardTypes.Nothing
        }
    }

    override fun KotlinTypeMarker.containsErrorComponent(): Boolean {
        return when (this) {
            is ConeFlexibleType -> {
                lowerBound.containsErrorComponent() || upperBound.containsErrorComponent()
            }
            is ConeErrorUnionType -> true
            else -> false
        }
    }

    private fun ConeRigidType.isPureErrorType(): Boolean {
        if (this !is ConeErrorUnionType) return false
        return valueType.isNothing
    }

    override fun KotlinTypeMarker.disableFlexibilityForErrorType(): ErrorTypeMarker {
        return when (this) {
            is ConeFlexibleType -> {
                require(lowerBound.isPureErrorType()) { "Not a pure error type" }
                require(upperBound.isPureErrorType()) { "Not a pure error type" }
                val lbT = (lowerBound as ConeErrorUnionType).errorType
                val ubT = (upperBound as ConeErrorUnionType).errorType
                check(lbT == ubT) { "Flexible type with different error components detected: $this" }
                return lbT
            }
            is ConeErrorUnionType -> {
                require(valueType.isNothing) { "Not a pure error type" }
                return errorType
            }
            else if this.isNothing() -> CEBotType
            else -> error("Not a pure error type")
        }
    }

    private fun CEType.isSubtypeOfAtomic(other: CEType): Boolean {
        require(other !is CEUnionType) { "Not atomic" }
        if (other is CETopType) return true
        return when (this) {
            is CEBotType -> true
            is CETopType -> false
            is CETypeParameterType -> {
                // TODO: RE: HIGH: this approach to subtyping is incorrect:
                // Type parameter may be a subtype of several atomics, while none of them separately
                // All subtyping-related places require refactoring
                lookupTag.bounds().any { bound ->
                    val errorProjection = bound.coneType.projectOnError()
                    when {
                        errorProjection.isNothing() -> true
                        errorProjection is ConeErrorUnionType ->
                            errorProjection.errorType.isSubtypeOf(other)
                        else -> error("Unreachable")
                    }
                }
            }
            is CELookupTagBasedType -> this == other
            is CETypeVariableType -> this == other
            is CEUnionType -> error("Not atomic")
        }
    }

    private fun <T : MutableCollection<CEType>> CEType.collectAtomics(col: T): T {
        when (this) {
            is CELookupTagBasedType -> col.add(this)
            is CETypeVariableType -> col.add(this)
            is CETopType -> col.add(this)
            is CEUnionType -> types.forEach { it.collectAtomics(col) }
            is CEBotType -> {}
        }
        return col
    }

    fun ErrorTypeMarker.isSubtypeOf(other: ErrorTypeMarker): Boolean {
        // TODO: RE: MID: for better and faster subtyping we should initially store them in some kind of sets
        val subTs = (this as CEType).collectAtomics(mutableListOf())
        val superTs = (other as CEType).collectAtomics(mutableListOf())

        return subTs.all { subT -> superTs.any { superT -> subT.isSubtypeOfAtomic(superT) } || subT.isSubtypeOfAtomic(CEBotType) }
    }

    override fun simplifyAndIncorporateSubtyping(
        lowerType: ErrorTypeMarker,
        upperType: ErrorTypeMarker,
    ): List<Pair<ErrorTypeMarker, ErrorTypeMarker>> {
        val subTs = (lowerType as CEType).collectAtomics(mutableListOf())
        val superTs = (upperType as CEType).collectAtomics(mutableListOf())

        val unsatisfiedSubTs = subTs.filter { subTs ->
            superTs.none { superTs -> subTs.isSubtypeOfAtomic(superTs) } &&
                    !subTs.isSubtypeOfAtomic(CEBotType)
        }

        val containedVariablesSub = subTs.filter { it is CETypeVariableType }

        if (unsatisfiedSubTs.isEmpty()) {
            val containedVariablesSuper = superTs.filter { it is CETypeVariableType }

            return buildList {
                containedVariablesSub.forEach { subV ->
                    add(subV to CETopType)
                }
                containedVariablesSuper.forEach { superV ->
                    add(superV to CETopType)
                }
            }
        }

        val lostSubV = containedVariablesSub.filter { subV ->
            !unsatisfiedSubTs.contains(subV)
        }

        return buildList {
            unsatisfiedSubTs.forEach { subT ->
                add(subT to upperType)
            }
            lostSubV.forEach { subV ->
                add(subV to CETopType)
            }
        }
    }

    override fun ErrorTypeMarker.isPossibleSubtypeOf(other: ErrorTypeMarker): Boolean {
        require(this !is CEUnionType) { "Expected atomic" }

        val subT = this as CEType
        val superTs = (other as CEType).collectAtomics(mutableListOf())

        if (subT is CETypeVariableType) return true
        if (superTs.any { it is CETypeVariableType }) return true

        return false
    }

    override fun List<ErrorTypeMarker>.intersectErrorTypes(): CEType {
        // TODO: RE: suspicious function
        val atomicTypes = this.map { (it as CEType).collectAtomics(mutableListOf()) }

        val resultingTypes = mutableListOf<CEType>()

        atomicTypes.forEach {
            it.forEach { type ->
                // already in intersection => not needed
                if (resultingTypes.any { superType -> type.isSubtypeOfAtomic(superType) })
                    return@forEach
                // check if this atom is a subtype of all types in intersection
                if (atomicTypes.all { types -> types.any { superType -> type.isSubtypeOfAtomic(superType) } })
                    resultingTypes.add(type)
            }
        }

        val extraTypes = atomicTypes.map { it.filter { atom -> resultingTypes.none { resAtom -> atom.isSubtypeOfAtomic(resAtom) } } }
        if (extraTypes.sumOf { it.count { it is CETypeVariableType || it is CETypeParameterType } } > 1) {
            error("Precise intersection is not possible. Some investigation required.")
        }

        return when (resultingTypes.size) {
            0 -> CEBotType
            else -> CEUnionType.create(resultingTypes)
        }
    }

    fun ConeRigidType.errorComponent(): CEType {
        return when (this) {
            is ConeErrorUnionType -> errorType
            is ConeValueType -> CEBotType
        }
    }

    fun ConeRigidType.valueComponent(): ConeValueType {
        return when (this) {
            is ConeErrorUnionType -> valueType
            is ConeValueType -> this
        }
    }

    override fun RichErrorsSystemState<ErrorTypeMarker>.solveSystem(): RichErrorsSystemSolution<ErrorTypeMarker> {
        // TODO: RE: Current solution considers all type variables as required to solve, which is not always true

        @Suppress("UNCHECKED_CAST")
        this as RichErrorsSystemState<CEType>

        require(constraints.all { it.lower !is CEUnionType })


        fun CEType.allVariables(): Sequence<CETypeVariableType> {
            return when (this) {
                is CEBotType -> emptySequence()
                is CELookupTagBasedType -> emptySequence()
                is CETopType -> emptySequence()
                is CETypeVariableType -> sequenceOf(this)
                is CEUnionType -> types.asSequence().flatMap { it.allVariables() }
            }
        }

        val variables = buildList {
            for (constraint in constraints) {
                constraint.lower.allVariables().forEach { add(it) }
                constraint.upper.allVariables().forEach { add(it) }
            }
        }

        val currentSolution = variables.map { it.typeConstructor }.associateWith { mutableListOf<CEType>() }

        fun CEType.atomicsWithSubstitution(): List<CEType> {
            return when (this) {
                is CEBotType -> emptyList()
                is CELookupTagBasedType -> listOf(this)
                is CETopType -> listOf(this)
                is CETypeVariableType -> currentSolution[typeConstructor]!!
                is CEUnionType -> types.flatMap { it.atomicsWithSubstitution() }
            }
        }

        fun RichErrorsSystemState.Constraint<CEType>.isSatisfied(): Boolean {
            val lowerAtomics = lower.atomicsWithSubstitution()
            val upperAtomics = upper.atomicsWithSubstitution()
            return lowerAtomics.all { lowerT -> upperAtomics.any { upperT -> lowerT.isSubtypeOfAtomic(upperT) } }
        }

        fun RichErrorsSystemState.Constraint<CEType>.isPossible(): Boolean {
            fun CEType.isPossibleSubtypeOf(other: CEType): Boolean {
                if (other is CETypeVariableType) return true
                if (isSubtypeOfAtomic(other)) return true
                return false
            }

            val lowerAtomics = lower.atomicsWithSubstitution()
            val upperAtomics = upper.collectAtomics(mutableListOf())
            return lowerAtomics.all { lowerT -> upperAtomics.any { upperT -> lowerT.isPossibleSubtypeOf(upperT) } }
        }

        val impossibleConstraints = mutableSetOf<RichErrorsSystemState.Constraint<CEType>>()

        do {
            var relaxedAny = false

            for (constraint in constraints) {
                if (constraint.isSatisfied() || constraint in impossibleConstraints) continue
                if (!constraint.isPossible()) {
                    impossibleConstraints.add(constraint)
                    continue
                }
                relaxedAny = true

                val lowerAtomics = constraint.lower.atomicsWithSubstitution()
                val upperAtomics = constraint.upper.collectAtomics(mutableListOf())

                val unsatisfiedLowerAtomics = lowerAtomics.filter { lt -> !upperAtomics.any { ut -> lt.isSubtypeOfAtomic(ut) } }

                val upperVariables = upperAtomics.filterIsInstance<CETypeVariableType>()
                val upperVariable = when (upperVariables.size) {
                    0 -> error("Should not happen")
                    1 -> upperVariables.single()
                    else -> error("Too many upper variables: $upperVariables")
                }

                currentSolution[upperVariable.typeConstructor]!!.addAll(unsatisfiedLowerAtomics)
            }
        } while (relaxedAny)

        // TODO: RE: Simplify current solution

        return RichErrorsSystemSolution(
            currentSolution.mapValues { (_, types) -> CEUnionType.create(types) },
            impossibleConstraints.isNotEmpty()
        )
    }

    override fun List<ErrorTypeMarker>.commonSupertypeForErrors(): ErrorTypeMarker {
        val atomicTypes = this.map { (it as CEType).collectAtomics(mutableListOf()) }

        val resultingTypes = mutableListOf<CEType>()

        atomicTypes.forEach {
            it.forEach { type ->
                // already in intersection => not needed
                if (resultingTypes.any { superType -> type.isSubtypeOfAtomic(superType) })
                    return@forEach
                resultingTypes.add(type)
            }
        }

        return when (resultingTypes.size) {
            0 -> CEBotType
            else -> CEUnionType.create(resultingTypes)
        }
    }

    override fun KotlinTypeMarker.addErrorComponent(errorType: ErrorTypeMarker): KotlinTypeMarker {
        return ConeErrorUnionType.addErrorComponent(this as ConeKotlinType, errorType as CEType)
    }

    fun CEType.typeConstructor(): TypeConstructorMarker {
        return when (this) {
            is CEBotType -> error("Unexpected")
            is CELookupTagBasedType -> lookupTag
            is CETopType -> error("Unexpected")
            is CETypeVariableType -> typeConstructor
            is CEUnionType -> error("Unexpected")
        }
    }

    override fun botTypeOfErrors(): ErrorTypeMarker {
        return CEBotType
    }

    override fun KotlinTypeMarker.isDefinitelyNotNullType(): Boolean {
        this as ConeKotlinType
        val rigid = if (this is ConeFlexibleType) lowerBound else this
        val value = if (rigid is ConeErrorUnionType) rigid.valueType else rigid
        return value is ConeDefinitelyNotNullType
    }

    override fun RigidTypeMarker.isDefinitelyNotNullType(): Boolean {
        this as ConeRigidType
        val value = if (this is ConeErrorUnionType) valueType else this
        return value is ConeDefinitelyNotNullType
    }
}

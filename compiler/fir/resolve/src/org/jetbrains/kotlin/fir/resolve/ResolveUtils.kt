/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.FirResolvedReifiedParameterReferenceBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.bindSymbolToLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

inline fun <K, V, VA : V> MutableMap<K, V>.getOrPut(key: K, defaultValue: (K) -> VA, postCompute: (VA) -> Unit): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue(key)
        put(key, answer)
        postCompute(answer)
        answer
    } else {
        value
    }
}

val FirSession.firSymbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor()
val FirSession.firProvider: FirProvider by FirSession.sessionComponentAccessor()
val FirSession.correspondingSupertypesCache: FirCorrespondingSupertypesCache by FirSession.sessionComponentAccessor()
val FirSession.declaredMemberScopeProvider: FirDeclaredMemberScopeProvider by FirSession.sessionComponentAccessor()
val FirSession.qualifierResolver: FirQualifierResolver by FirSession.sessionComponentAccessor()
val FirSession.typeResolver: FirTypeResolver by FirSession.sessionComponentAccessor()
val FirSession.effectiveVisibilityResolver: FirEffectiveVisibilityResolver by FirSession.sessionComponentAccessor()

fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    if (this is ConeClassLookupTagWithFixedSymbol) {
        return this.symbol
    }
    val firSymbolProvider = useSiteSession.firSymbolProvider
    return firSymbolProvider.getSymbolByLookupTag(this)
}

fun ConeClassLikeType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType,
): ConeClassLikeType {
    if (this is ConeClassLikeTypeImpl) {
        val expandedTypeAndSession = cachedExpandedType
        if (expandedTypeAndSession != null && expandedTypeAndSession.first === useSiteSession) {
            return expandedTypeAndSession.second
        }

        val computedExpandedType = fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
        cachedExpandedType = Pair(useSiteSession, computedExpandedType)
        return computedExpandedType
    }

    return fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
}

fun ConeKotlinType.fullyExpandedType(
    useSiteSession: FirSession
): ConeKotlinType = when (this) {
    is ConeFlexibleType ->
        ConeFlexibleType(lowerBound.fullyExpandedType(useSiteSession), upperBound.fullyExpandedType(useSiteSession))
    is ConeClassLikeType -> fullyExpandedType(useSiteSession)
    else -> this
}

private fun ConeClassLikeType.fullyExpandedTypeNoCache(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType?,
): ConeClassLikeType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    return directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
}

fun ConeClassLikeType.directExpansionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType,
): ConeClassLikeType? {
    val typeAliasSymbol = lookupTag.toSymbol(useSiteSession) as? FirTypeAliasSymbol ?: return null
    val typeAlias = typeAliasSymbol.fir

    val resultType = expandedConeType(typeAlias)?.applyNullabilityFrom(this) ?: return null

    if (resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType) as? ConeClassLikeType
}

private fun ConeClassLikeType.applyNullabilityFrom(abbreviation: ConeClassLikeType): ConeClassLikeType {
    if (abbreviation.isMarkedNullable) return withNullability(ConeNullability.NULLABLE)
    return this
}

private fun mapTypeAliasArguments(
    typeAlias: FirTypeAlias,
    abbreviatedType: ConeClassLikeType,
    resultingType: ConeClassLikeType,
): ConeKotlinType {
    val typeAliasMap = typeAlias.typeParameters.map { it.symbol }.zip(abbreviatedType.typeArguments).toMap()

    val substitutor = object : AbstractConeSubstitutor() {
        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

        override fun substituteArgument(projection: ConeTypeProjection): ConeTypeProjection? {
            val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
            val symbol = (type as? ConeTypeParameterType)?.lookupTag?.toSymbol() ?: return super.substituteArgument(projection)
            val mappedProjection = typeAliasMap[symbol] ?: return super.substituteArgument(projection)
            val mappedType = (mappedProjection as? ConeKotlinTypeProjection)?.type ?: return mappedProjection

            @Suppress("MoveVariableDeclarationIntoWhen")
            val resultingKind = mappedProjection.kind + projection.kind
            return when (resultingKind) {
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(mappedType)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(mappedType)
                ProjectionKind.INVARIANT -> mappedType
            }
        }
    }

    return substitutor.substituteOrSelf(resultingType)
}

fun ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): FirClassifierSymbol<*>? =
    when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeTypeParameterLookupTag -> this.symbol
        else -> error("sealed ${this::class}")
    }

fun ConeTypeParameterLookupTag.toSymbol(): FirTypeParameterSymbol = this.symbol as FirTypeParameterSymbol

fun FirClassifierSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is FirTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this.toLookupTag(), isNullable, attributes)
        }
        is FirClassSymbol -> {
            ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isNullable, attributes)
        }
        is FirTypeAliasSymbol -> {
            ConeClassLikeTypeImpl(
                this.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = isNullable,
                attributes = attributes
            )
        }
        else -> error("!")
    }
}

fun FirClassifierSymbol<*>.constructType(parts: List<FirQualifierPart>, isNullable: Boolean): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

fun FirFunction<*>.constructFunctionalTypeRef(isSuspend: Boolean = false): FirResolvedTypeRef {
    val receiverTypeRef = when (this) {
        is FirSimpleFunction -> receiverTypeRef
        is FirAnonymousFunction -> receiverTypeRef
        else -> null
    }
    val parameters = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter")
    }
    val rawReturnType = (this as FirTypedDeclaration).returnTypeRef.coneType

    val functionalType = createFunctionalType(parameters, receiverTypeRef?.coneType, rawReturnType, isSuspend = isSuspend)

    return buildResolvedTypeRef {
        source = this@constructFunctionalTypeRef.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
        type = functionalType
        this.isSuspend = isSuspend
    }
}

fun createFunctionalType(
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isSuspend: Boolean,
    isKFunctionType: Boolean = false
): ConeLookupTagBasedType {
    val receiverAndParameterTypes = listOfNotNull(receiverType) + parameters + listOf(rawReturnType)

    val kind = if (isSuspend) {
        if (isKFunctionType) FunctionClassDescriptor.Kind.KSuspendFunction else FunctionClassDescriptor.Kind.SuspendFunction
    } else {
        if (isKFunctionType) FunctionClassDescriptor.Kind.KFunction else FunctionClassDescriptor.Kind.Function
    }

    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(receiverAndParameterTypes.size - 1))
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), receiverAndParameterTypes.toTypedArray(), isNullable = false)
}

fun createKPropertyType(
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isMutable: Boolean,
): ConeLookupTagBasedType {
    val arguments = if (receiverType != null) listOf(receiverType, rawReturnType) else listOf(rawReturnType)
    val classId = StandardClassIds.reflectByName("K${if (isMutable) "Mutable" else ""}Property${arguments.size - 1}")
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(classId), arguments.toTypedArray(), isNullable = false)
}

fun BodyResolveComponents.buildResolvedQualifierForClass(
    regularClass: FirClassLikeSymbol<*>,
    sourceElement: FirSourceElement? = null,
    // TODO: Clarify if we actually need type arguments for qualifier?
    typeArgumentsForQualifier: List<FirTypeProjection> = emptyList()
): FirResolvedQualifier {
    val classId = regularClass.classId
    return buildResolvedQualifier {
        source = sourceElement
        packageFqName = classId.packageFqName
        relativeClassFqName = classId.relativeClassName
        typeArguments.addAll(typeArgumentsForQualifier)
        symbol = regularClass
    }.apply {
        resultType = if (classId.isLocal) {
            typeForQualifierByDeclaration(regularClass.fir, resultType, session)
                ?: session.builtinTypes.unitType
        } else {
            typeForQualifier(this)
        }
    }
}

fun BodyResolveComponents.typeForQualifier(resolvedQualifier: FirResolvedQualifier): FirTypeRef {
    val classSymbol = resolvedQualifier.symbol
    val resultType = resolvedQualifier.resultType
    if (classSymbol != null) {
        val declaration = classSymbol.phasedFir
        if (declaration !is FirTypeAlias || resolvedQualifier.typeArguments.isEmpty()) {
            typeForQualifierByDeclaration(declaration, resultType, session)?.let { return it }
        }
    }
    // TODO: Handle no value type here
    return session.builtinTypes.unitType
}

internal fun typeForReifiedParameterReference(parameterReferenceBuilder: FirResolvedReifiedParameterReferenceBuilder): FirTypeRef {
    val resultType = parameterReferenceBuilder.typeRef
    val typeParameterSymbol = parameterReferenceBuilder.symbol
    return resultType.resolvedTypeFromPrototype(typeParameterSymbol.constructType(emptyArray(), false))
}

internal fun typeForQualifierByDeclaration(declaration: FirDeclaration, resultType: FirTypeRef, session: FirSession): FirTypeRef? {
    if (declaration is FirTypeAlias) {
        val expandedDeclaration = declaration.expandedConeType?.lookupTag?.toSymbol(session)?.fir ?: return null
        return typeForQualifierByDeclaration(expandedDeclaration, resultType, session)
    }
    if (declaration is FirRegularClass) {
        if (declaration.classKind == ClassKind.OBJECT) {
            return resultType.resolvedTypeFromPrototype(
                declaration.symbol.constructType(emptyArray(), false),
            )
        } else {
            val companionObject = declaration.companionObject
            if (companionObject != null) {
                return resultType.resolvedTypeFromPrototype(
                    companionObject.symbol.constructType(emptyArray(), false),
                )
            }
        }
    }
    return null
}

fun <T : FirResolvable> BodyResolveComponents.typeFromCallee(access: T): FirResolvedTypeRef {
    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            buildErrorTypeRef {
                source = access.source?.fakeElement(FirFakeSourceElementKind.ErrorTypeRef)
                diagnostic = ConeStubDiagnostic(newCallee.diagnostic)
            }
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, false)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(newCallee.resolvedSymbol, false)
        }
        is FirThisReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver = implicitReceiverStack[labelName]
            buildResolvedTypeRef {
                source = null
                type = implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName")
            }
        }
        is FirSuperReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver =
                if (labelName != null) implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
                else implicitReceiverStack.lastDispatchReceiver()
            val resolvedTypeRef =
                newCallee.superTypeRef as? FirResolvedTypeRef
                    ?: implicitReceiver?.boundSymbol?.phasedFir?.superTypeRefs?.singleOrNull() as? FirResolvedTypeRef
            resolvedTypeRef ?: buildErrorTypeRef {
                source = newCallee.source
                diagnostic = ConeUnresolvedNameError(Name.identifier("super"))
            }
        }
        else -> error("Failed to extract type from: $newCallee")
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: AbstractFirBasedSymbol<*>, makeNullable: Boolean): FirResolvedTypeRef {
    return when (symbol) {
        is FirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.phasedFir)
            if (makeNullable) {
                returnTypeRef.withReplacedConeType(
                    returnTypeRef.type.withNullability(ConeNullability.NULLABLE, session.typeContext),
                )
            } else {
                returnTypeRef
            }
        }
        is FirClassifierSymbol<*> -> {
            // TODO: unhack
            buildResolvedTypeRef {
                source = null
                type = symbol.constructType(emptyArray(), isNullable = false)
            }
        }
        else -> error("WTF ! $symbol")
    }
}

fun BodyResolveComponents.transformQualifiedAccessUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): FirQualifiedAccessExpression {
    val typesFromSmartCast = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression) ?: return qualifiedAccessExpression
    val allTypes = typesFromSmartCast.also {
        it += qualifiedAccessExpression.resultType.coneType
    }
    val intersectedType = ConeTypeIntersector.intersectTypes(inferenceComponents.ctx, allTypes)
    // TODO: add check that intersectedType is not equal to original type
    val intersectedTypeRef = buildResolvedTypeRef {
        source = qualifiedAccessExpression.resultType.source?.fakeElement(FirFakeSourceElementKind.SmartCastedTypeRef)
        type = intersectedType
        annotations += qualifiedAccessExpression.resultType.annotations
    }
    return buildExpressionWithSmartcast {
        originalExpression = qualifiedAccessExpression
        typeRef = intersectedTypeRef
        this.typesFromSmartCast = typesFromSmartCast
    }
}

fun CallableId.isInvoke() =
    isKFunctionInvoke()
            || callableName.asString() == "invoke"
            && className?.asString()?.startsWith("Function") == true
            && packageName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME

fun CallableId.isKFunctionInvoke() =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("KFunction") == true
            && packageName.asString() == "kotlin.reflect"

fun CallableId.isIteratorNext() =
    callableName.asString() == "next" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIteratorHasNext() =
    callableName.asString() == "hasNext" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIterator() =
    callableName.asString() == "iterator" && packageName.asString() == "kotlin.collections"

fun FirAnnotationCall.fqName(session: FirSession): FqName? {
    val symbol = session.firSymbolProvider.getSymbolByTypeRef<FirRegularClassSymbol>(annotationTypeRef) ?: return null
    return symbol.classId.asSingleFqName()
}

fun FirCheckedSafeCallSubject.propagateTypeFromOriginalReceiver(nullableReceiverExpression: FirExpression, session: FirSession) {
    val receiverType = nullableReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>() ?: return

    val expandedReceiverType = if (receiverType is ConeClassLikeType) receiverType.fullyExpandedType(session) else receiverType

    replaceTypeRef(typeRef.resolvedTypeFromPrototype(expandedReceiverType.makeConeTypeDefinitelyNotNullOrNotNull()))
}

fun FirSafeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(
    nullableReceiverExpression: FirExpression,
    session: FirSession,
) {
    val receiverType = nullableReceiverExpression.typeRef.coneTypeSafe<ConeKotlinType>()
    val typeAfterNullCheck = regularQualifiedAccess.expressionTypeOrUnitForAssignment() ?: return
    val isReceiverActuallyNullable = receiverType != null && session.typeContext.run { receiverType.isNullableType() }

    val resultingType =
        if (isReceiverActuallyNullable)
            typeAfterNullCheck.withNullability(ConeNullability.NULLABLE, session.typeContext)
        else
            typeAfterNullCheck

    replaceTypeRef(typeRef.resolvedTypeFromPrototype(resultingType))
}

private fun FirQualifiedAccess.expressionTypeOrUnitForAssignment(): ConeKotlinType? {
    if (this is FirExpression) return typeRef.coneTypeSafe()

    require(this is FirVariableAssignment) {
        "The only non-expression FirQualifiedAccess is FirVariableAssignment, but ${this::class} was found"
    }
    return StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false)
}

fun FirAnnotationCall.getCorrespondingConstructorReferenceOrNull(session: FirSession): FirResolvedNamedReference? =
    annotationTypeRef.coneType.classId?.let {
        if (it.isLocal) {
            // TODO: How to retrieve local annotaiton's constructor?
            null
        } else {
            (session.firSymbolProvider.getClassLikeSymbolByFqName(it) as? FirRegularClassSymbol)?.fir
                ?.getPrimaryConstructorIfAny()
                ?.let { annotationConstructor ->
                    buildResolvedNamedReference {
                        source = this@getCorrespondingConstructorReferenceOrNull.source
                        name = it.shortClassName
                        resolvedSymbol = annotationConstructor.symbol
                    }
                }
        }
    }

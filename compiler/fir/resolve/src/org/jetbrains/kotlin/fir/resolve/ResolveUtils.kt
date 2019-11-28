/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastImpl
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

val FirSession.firSymbolProvider: FirSymbolProvider by componentArrayAccessor()
val FirSession.firProvider: FirProvider by componentArrayAccessor()
val FirSession.correspondingSupertypesCache: FirCorrespondingSupertypesCache by componentArrayAccessor()
val FirSession.memberScopeProvider: FirMemberScopeProvider by componentArrayAccessor()

fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    if (this is ConeClassLookupTagWithFixedSymbol) {
        return this.symbol
    }
    val firSymbolProvider = useSiteSession.firSymbolProvider
    return firSymbolProvider.getSymbolByLookupTag(this)
}

fun ConeClassLikeType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType
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

private fun ConeClassLikeType.fullyExpandedTypeNoCache(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType?
): ConeClassLikeType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    return directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
}

fun ConeClassLikeType.directExpansionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType
): ConeClassLikeType? {
    val typeAlias = lookupTag
        .toSymbol(useSiteSession)
        ?.safeAs<FirTypeAliasSymbol>()?.fir ?: return null

    val resultType = expandedConeType(typeAlias) ?: return null
    if (resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType) as? ConeClassLikeType
}

private fun mapTypeAliasArguments(
    typeAlias: FirTypeAlias,
    abbreviatedType: ConeClassLikeType,
    resultingType: ConeClassLikeType
): ConeKotlinType {
    val typeAliasMap = typeAlias.typeParameters.map { it.symbol }.zip(abbreviatedType.typeArguments).toMap()

    val substitutor = object : AbstractConeSubstitutor() {
        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

        override fun substituteArgument(projection: ConeKotlinTypeProjection): ConeKotlinTypeProjection? {
            val type = (projection as? ConeTypedProjection)?.type ?: return null
            val symbol = (type as? ConeTypeParameterType)?.lookupTag?.toSymbol() ?: return super.substituteArgument(projection)
            val mappedProjection = typeAliasMap[symbol] ?: return super.substituteArgument(projection)
            val mappedType = (mappedProjection as? ConeTypedProjection)?.type ?: return mappedProjection
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

fun ConeClassLikeLookupTag.constructClassType(typeArguments: Array<out ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return ConeClassLikeTypeImpl(this, typeArguments, isNullable)
}

fun ConeClassifierLookupTag.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isNullable)
        else -> error("! ${this::class}")
    }
}

fun FirClassifierSymbol<*>.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is FirTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this.toLookupTag(), isNullable)
        }
        is FirClassSymbol -> {
            ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isNullable)
        }
        is FirTypeAliasSymbol -> {
            ConeClassLikeTypeImpl(
                this.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = isNullable
            )
        }
        else -> error("!")
    }
}

fun FirClassifierSymbol<*>.constructType(parts: List<FirQualifierPart>, isNullable: Boolean): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)

fun FirClassifierSymbol<*>.constructType(
    parts: List<FirQualifierPart>,
    isNullable: Boolean,
    symbolOriginSession: FirSession
): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)
        .also {
            val lookupTag = it.lookupTag
            if (lookupTag is ConeClassLikeLookupTagImpl && this is FirClassLikeSymbol<*>) {
                lookupTag.bindSymbolToLookupTag(symbolOriginSession.firSymbolProvider, this)
            }
        }

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeKotlinTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

private fun List<FirQualifierPart>.toTypeProjections(): Array<ConeKotlinTypeProjection> = flatMap {
    it.typeArguments.map { typeArgument ->
        when (typeArgument) {
            is FirStarProjection -> ConeStarProjection
            is FirTypeProjectionWithVariance -> {
                val type = (typeArgument.typeRef as FirResolvedTypeRef).type
                type.toTypeProjection(typeArgument.variance)
            }
            else -> error("!")
        }
    }
}.toTypedArray()

fun coneFlexibleOrSimpleType(
    typeContext: ConeInferenceContext?,
    lowerBound: ConeKotlinType,
    upperBound: ConeKotlinType
): ConeKotlinType {
    if (lowerBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound.lowerBound, upperBound)
    }
    if (upperBound is ConeFlexibleType) {
        return coneFlexibleOrSimpleType(typeContext, lowerBound, upperBound.upperBound)
    }
    if (typeContext != null && AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, lowerBound, upperBound)) {
        return lowerBound
    }
    return ConeFlexibleType(lowerBound, upperBound)
}

fun <T : ConeKotlinType> T.withNullability(nullability: ConeNullability, typeContext: ConeInferenceContext? = null): T {
    if (this.nullability == nullability) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable) as T
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable) as T
        is ConeFlexibleType -> {
            if (nullability == ConeNullability.UNKNOWN) {
                if (lowerBound.nullability != upperBound.nullability || lowerBound.nullability == ConeNullability.UNKNOWN) {
                    return this
                }
            }
            coneFlexibleOrSimpleType(typeContext, lowerBound.withNullability(nullability), upperBound.withNullability(nullability)) as T
        }
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag) as T
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor) as T
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability)
            }
            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        } as T
        is ConeStubType -> ConeStubType(variable, nullability) as T
        is ConeDefinitelyNotNullType -> when (nullability) {
            ConeNullability.NOT_NULL -> this
            ConeNullability.NULLABLE -> original.withNullability(nullability)
            ConeNullability.UNKNOWN -> original.withNullability(nullability)
        } as T
        is ConeIntegerLiteralType -> this
        else -> error("sealed: ${this::class}")
    }
}


fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeKotlinTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType.create(original.withArguments(arguments)) as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}

fun FirFunction<*>.constructFunctionalTypeRef(session: FirSession): FirResolvedTypeRef {
    val receiverTypeRef = when (this) {
        is FirSimpleFunction -> receiverTypeRef
        is FirAnonymousFunction -> receiverTypeRef
        else -> null
    }
    val parameters = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter")
    }
    val rawReturnType = (this as FirTypedDeclaration).returnTypeRef.coneTypeUnsafe<ConeKotlinType>()

    val functionalType = createFunctionalType(parameters, receiverTypeRef?.coneTypeSafe(), rawReturnType)

    return FirResolvedTypeRefImpl(source, functionalType)
}

fun createFunctionalType(
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isKFunctionType: Boolean = false
): ConeLookupTagBasedType {
    val receiverAndParameterTypes = listOfNotNull(receiverType) + parameters + listOf(rawReturnType)

    val postfix = "Function${receiverAndParameterTypes.size - 1}"
    val functionalTypeId = if (isKFunctionType) StandardClassIds.reflectByName("K$postfix") else StandardClassIds.byName(postfix)
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), receiverAndParameterTypes.toTypedArray(), isNullable = false)
}

fun createKPropertyType(
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isMutable: Boolean
): ConeLookupTagBasedType {
    val arguments = if (receiverType != null) listOf(receiverType, rawReturnType) else listOf(rawReturnType)
    val classId = StandardClassIds.reflectByName("K${if (isMutable) "Mutable" else ""}Property${arguments.size - 1}")
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(classId), arguments.toTypedArray(), isNullable = false)
}

fun BodyResolveComponents.typeForQualifier(resolvedQualifier: FirResolvedQualifier): FirTypeRef {
    val classId = resolvedQualifier.classId
    val resultType = resolvedQualifier.resultType
    if (classId != null) {
        val classSymbol = symbolProvider.getClassLikeSymbolByFqName(classId)
            ?: return FirErrorTypeRefImpl(source = null, diagnostic = FirSimpleDiagnostic("No type for qualifier", DiagnosticKind.Other))
        val declaration = classSymbol.phasedFir
        typeForQualifierByDeclaration(declaration, resultType)?.let { return it }
    }
    // TODO: Handle no value type here
    return resultType.resolvedTypeFromPrototype(
        session.builtinTypes.unitType.type
    )
}

internal fun typeForReifiedParameterReference(parameterReference: FirResolvedReifiedParameterReference): FirTypeRef {
    val resultType = parameterReference.resultType
    val typeParameterSymbol = parameterReference.symbol
    return resultType.resolvedTypeFromPrototype(typeParameterSymbol.constructType(emptyArray(), false))
}

internal fun typeForQualifierByDeclaration(declaration: FirDeclaration, resultType: FirTypeRef): FirTypeRef? {
    if (declaration is FirRegularClass) {
        if (declaration.classKind == ClassKind.OBJECT) {
            return resultType.resolvedTypeFromPrototype(
                declaration.symbol.constructType(emptyArray(), false)
            )
        } else {
            val companionObject = declaration.companionObject
            if (companionObject != null) {
                return resultType.resolvedTypeFromPrototype(
                    companionObject.symbol.constructType(emptyArray(), false)
                )
            }
        }
    }
    return null
}

fun <T : FirResolvable> BodyResolveComponents.typeFromCallee(access: T): FirResolvedTypeRef {
    val makeNullable: Boolean by lazy {
        if (access is FirQualifiedAccess && access.safe) {
            val explicitReceiver = access.explicitReceiver!!
            val receiverResultType = explicitReceiver.resultType
            if (receiverResultType is FirResolvedTypeRef) {
                receiverResultType.type.isNullable
            } else {
                throw AssertionError("Receiver ${explicitReceiver.render()} type is unresolved: ${receiverResultType.render()}")
            }
        } else {
            false
        }
    }

    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            FirErrorTypeRefImpl(access.source, FirStubDiagnostic(newCallee.diagnostic))
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, makeNullable)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(newCallee.resolvedSymbol, makeNullable)
        }
        is FirThisReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver = implicitReceiverStack[labelName]
            FirResolvedTypeRefImpl(null, implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName"))
        }
        is FirSuperReference -> {
            newCallee.superTypeRef as? FirResolvedTypeRef ?: FirErrorTypeRefImpl(newCallee.source, FirUnresolvedNameError(Name.identifier("super")))
        }
        else -> error("Failed to extract type from: $newCallee")
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: AbstractFirBasedSymbol<*>, makeNullable: Boolean): FirResolvedTypeRef {
    return when (symbol) {
        is FirCallableSymbol<*> -> {
            val returnType = returnTypeCalculator.tryCalculateReturnType(symbol.phasedFir)
            if (makeNullable) {
                returnType.withReplacedConeType(
                    returnType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE, session.inferenceContext)
                )
            } else {
                returnType
            }
        }
        is FirClassifierSymbol<*> -> {
            val fir = (symbol as? AbstractFirBasedSymbol<*>)?.phasedFir
            // TODO: unhack
            FirResolvedTypeRefImpl(
                null, symbol.constructType(emptyArray(), isNullable = false)
            )
        }
        else -> error("WTF ! $symbol")
    }
}

fun BodyResolveComponents.transformQualifiedAccessUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): FirQualifiedAccessExpression {
    val typesFromSmartCast = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression) ?: return qualifiedAccessExpression
    val allTypes = typesFromSmartCast.toMutableList().also {
        it += qualifiedAccessExpression.resultType.coneTypeUnsafe<ConeKotlinType>()
    }
    val intersectedType = ConeTypeIntersector.intersectTypes(inferenceComponents.ctx as ConeInferenceContext, allTypes)
    // TODO: add check that intersectedType is not equal to original type
    val intersectedTypeRef = FirResolvedTypeRefImpl(qualifiedAccessExpression.resultType.source, intersectedType).apply {
        annotations += qualifiedAccessExpression.resultType.annotations
    }
    return FirExpressionWithSmartcastImpl(qualifiedAccessExpression, intersectedTypeRef, typesFromSmartCast)
}

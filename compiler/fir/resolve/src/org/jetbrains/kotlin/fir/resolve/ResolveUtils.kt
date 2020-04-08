/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.FirResolvedReifiedParameterReferenceBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.isSuperReferenceExpression
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
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
import org.jetbrains.kotlin.name.Name
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
val FirSession.declaredMemberScopeProvider: FirDeclaredMemberScopeProvider by componentArrayAccessor()

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
    val typeAlias = lookupTag
        .toSymbol(useSiteSession)
        ?.safeAs<FirTypeAliasSymbol>()?.fir ?: return null

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

fun FirClassifierSymbol<*>.constructType(typeArguments: Array<ConeTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
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
                isNullable = isNullable,
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
    symbolOriginSession: FirSession,
): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)
        .also {
            val lookupTag = it.lookupTag
            if (lookupTag is ConeClassLikeLookupTagImpl && this is FirClassLikeSymbol<*>) {
                lookupTag.bindSymbolToLookupTag(symbolOriginSession.firSymbolProvider, this)
            }
        }


private fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> = flatMap {
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

    return buildResolvedTypeRef {
        source = this@constructFunctionalTypeRef.source
        type = functionalType
    }
}

fun createFunctionalType(
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType,
    isKFunctionType: Boolean = false,
    isSuspend: Boolean = false
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
        safe = false
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
    val makeNullable: Boolean by lazy {
        if (access is FirQualifiedAccess && access.safe) {
            val explicitReceiver = access.explicitReceiver
                ?: throw AssertionError("Safe call without explicit receiver: ${access.render()}")
            val receiverResultType = explicitReceiver.resultType
            if (receiverResultType is FirResolvedTypeRef) {
                receiverResultType.type.isNullable
            } else if (receiverResultType !is FirComposedSuperTypeRef || !explicitReceiver.isSuperReferenceExpression()){
                throw AssertionError("Receiver ${explicitReceiver.render()} type is unresolved: ${receiverResultType.render()}")
            } else {
                false
            }
        } else {
            false
        }
    }

    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            buildErrorTypeRef {
                source = access.source
                diagnostic = ConeStubDiagnostic(newCallee.diagnostic)
            }
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, makeNullable)
        }
        is FirResolvedNamedReference -> {
            typeFromSymbol(newCallee.resolvedSymbol, makeNullable)
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
            newCallee.superTypeRef as? FirResolvedTypeRef ?: buildErrorTypeRef {
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
            val returnType = returnTypeCalculator.tryCalculateReturnType(symbol.phasedFir)
            if (makeNullable) {
                returnType.withReplacedConeType(
                    returnType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE, session.inferenceContext),
                )
            } else {
                returnType
            }
        }
        is FirClassifierSymbol<*> -> {
            val fir = (symbol as? AbstractFirBasedSymbol<*>)?.phasedFir
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
        it += qualifiedAccessExpression.resultType.coneTypeUnsafe<ConeKotlinType>()
    }
    val intersectedType = ConeTypeIntersector.intersectTypes(inferenceComponents.ctx, allTypes)
    // TODO: add check that intersectedType is not equal to original type
    val intersectedTypeRef = buildResolvedTypeRef {
        source = qualifiedAccessExpression.resultType.source
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

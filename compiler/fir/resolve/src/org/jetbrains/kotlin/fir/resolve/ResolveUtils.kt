/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.resultType
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
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
val FirSession.correspondingSupertypesCache: FirCorrespondingSupertypesCache by componentArrayAccessor()

fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): ConeClassifierSymbol? {
    val firSymbolProvider = useSiteSession.firSymbolProvider
    return firSymbolProvider.getSymbolByLookupTag(this)
}

fun ConeAbbreviatedType.directExpansionType(useSiteSession: FirSession): ConeClassLikeType? {
    val typeAlias = abbreviationLookupTag
        .toSymbol(useSiteSession)
        ?.safeAs<FirTypeAliasSymbol>()?.fir ?: return null

    val resultType = typeAlias.expandedConeType ?: return null
    if (resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType) as? ConeClassLikeType
}

private fun mapTypeAliasArguments(typeAlias: FirTypeAlias, abbreviatedType: ConeAbbreviatedType, resultingType: ConeClassLikeType): ConeKotlinType {
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

fun ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): ConeClassifierSymbol? =
    when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeTypeParameterLookupTag -> this.symbol
        else -> error("sealed ${this::class}")
    }

fun ConeTypeParameterLookupTag.toSymbol(): FirTypeParameterSymbol = this.symbol as FirTypeParameterSymbol

fun ConeClassLikeLookupTag.constructClassType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return ConeClassTypeImpl(this, typeArguments, isNullable)
}

fun ConeClassifierLookupTag.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isNullable)
        else -> error("! ${this::class}")
    }
}

fun ConeClassifierSymbol.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this.toLookupTag(), isNullable)
        }
        is ConeClassSymbol -> {
            ConeClassTypeImpl(this.toLookupTag(), typeArguments, isNullable)
        }
        is FirTypeAliasSymbol -> {
            ConeAbbreviatedTypeImpl(
                abbreviationLookupTag = this.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = isNullable
            )
        }
        else -> error("!")
    }
}

fun ConeClassifierSymbol.constructType(parts: List<FirQualifierPart>, isNullable: Boolean): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)

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


fun <T : ConeKotlinType> T.withNullability(nullability: ConeNullability): T {
    if (this.nullability == nullability) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassTypeImpl -> ConeClassTypeImpl(lookupTag, typeArguments, nullability.isNullable) as T
        is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
            abbreviationLookupTag,
            typeArguments,
            nullability.isNullable
        ) as T
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable) as T
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withNullability(nullability), upperBound.withNullability(nullability)) as T
        is ConeTypeVariableType -> ConeTypeVariableType(nullability, lookupTag) as T
        is ConeCapturedType -> ConeCapturedType(captureStatus, lowerType, nullability, constructor) as T
        is ConeIntersectionType -> when (nullability) {
            ConeNullability.NULLABLE -> this.mapTypes {
                it.withNullability(nullability)
            }
            ConeNullability.UNKNOWN -> this // TODO: is that correct?
            ConeNullability.NOT_NULL -> this
        } as T
        else -> error("sealed: ${this::class}")
    }
}


fun <T : ConeKotlinType> T.withArguments(arguments: Array<out ConeKotlinTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassTypeImpl -> ConeClassTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
            abbreviationLookupTag,
            arguments,
            nullability.isNullable
        ) as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}

fun FirFunction<*>.constructFunctionalTypeRef(session: FirSession): FirResolvedTypeRef {
    val receiverTypeRef = when (this) {
        is FirNamedFunction -> receiverTypeRef
        is FirAnonymousFunction -> receiverTypeRef
        else -> null
    }
    val parameters = valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter")
    }
    val rawReturnType = (this as FirTypedDeclaration).returnTypeRef.coneTypeUnsafe<ConeKotlinType>()

    val functionalType = createFunctionalType(session, parameters, receiverTypeRef?.coneTypeUnsafe(), rawReturnType)

    return FirResolvedTypeRefImpl(psi, functionalType)
}

fun createFunctionalType(
    session: FirSession,
    parameters: List<ConeKotlinType>,
    receiverType: ConeKotlinType?,
    rawReturnType: ConeKotlinType
): ConeLookupTagBasedType {
    val receiverAndParameterTypes = listOfNotNull(receiverType) + parameters + listOf(rawReturnType)

    val functionalTypeId = StandardClassIds.byName("Function${receiverAndParameterTypes.size - 1}")
    val functionalType = functionalTypeId(session.service()).constructType(receiverAndParameterTypes.toTypedArray(), isNullable = false)
    return functionalType
}

fun BodyResolveComponents.typeForQualifier(resolvedQualifier: FirResolvedQualifier): FirTypeRef {
    val classId = resolvedQualifier.classId
    val resultType = resolvedQualifier.resultType
    if (classId != null) {
        val classSymbol = symbolProvider.getClassLikeSymbolByFqName(classId)!!
        val declaration = classSymbol.phasedFir
        if (declaration is FirClass) {
            if (declaration.classKind == ClassKind.OBJECT) {
                return resultType.resolvedTypeFromPrototype(
                    classSymbol.constructType(emptyArray(), false)
                )
            } else if (declaration.classKind == ClassKind.ENUM_ENTRY) {
                val enumClassSymbol = symbolProvider.getClassLikeSymbolByFqName(classSymbol.classId.outerClassId!!)!!
                return resultType.resolvedTypeFromPrototype(
                    enumClassSymbol.constructType(emptyArray(), false)
                )
            } else {
                if (declaration is FirRegularClass) {
                    val companionObject = declaration.companionObject
                    if (companionObject != null) {
                        return resultType.resolvedTypeFromPrototype(
                            companionObject.symbol.constructType(emptyArray(), false)
                        )
                    }
                }
            }
        }
    }
    // TODO: Handle no value type here
    return resultType.resolvedTypeFromPrototype(
        StandardClassIds.Unit(symbolProvider).constructType(emptyArray(), isNullable = false)
    )
}

fun <T : FirResolvable> BodyResolveComponents.typeFromCallee(access: T): FirResolvedTypeRef {
    val makeNullable: Boolean by lazy {
        if (access is FirQualifiedAccess)
            access.safe && access.explicitReceiver!!.resultType.coneTypeUnsafe<ConeKotlinType>().isNullable
        else
            false
    }

    return when (val newCallee = access.calleeReference) {
        is FirErrorNamedReference ->
            FirErrorTypeRefImpl(access.psi, newCallee.errorReason)
        is FirNamedReferenceWithCandidate -> {
            typeFromSymbol(newCallee.candidateSymbol, makeNullable)
        }
        is FirResolvedCallableReference -> {
            typeFromSymbol(newCallee.coneSymbol, makeNullable)
        }
        is FirThisReference -> {
            val labelName = newCallee.labelName
            val implicitReceiver = implicitReceiverStack[labelName]
            FirResolvedTypeRefImpl(null, implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName"), emptyList())
        }
        else -> error("Failed to extract type from: $newCallee")
    }
}

private fun BodyResolveComponents.typeFromSymbol(symbol: ConeSymbol, makeNullable: Boolean): FirResolvedTypeRef {
    return when (symbol) {
        is FirCallableSymbol<*> -> {
            val returnType = returnTypeCalculator.tryCalculateReturnType(symbol.phasedFir)
            if (makeNullable) {
                returnType.withReplacedConeType(
                    returnType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE)
                )
            } else {
                returnType
            }
        }
        is ConeClassifierSymbol -> {
            val fir = (symbol as? AbstractFirBasedSymbol<*>)?.phasedFir
            // TODO: unhack
            if (fir is FirEnumEntry) {
                (fir.superTypeRefs.firstOrNull() as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(
                    null,
                    "no enum item supertype"
                )
            } else
                FirResolvedTypeRefImpl(
                    null, symbol.constructType(emptyArray(), isNullable = false),
                    annotations = emptyList()
                )
        }
        else -> error("WTF ! $symbol")
    }
}
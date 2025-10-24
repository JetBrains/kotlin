/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import kotlin.Pair

abstract class SupertypeSupplier {
    abstract fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType>
    abstract fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType?

    object Default : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            if (!firClass.isLocal) {
                // for local classes the phase may not be updated till that moment
                firClass.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            }
            return firClass.superConeTypes
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            typeAlias.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            return typeAlias.expandedConeType
        }
    }
}

fun collectSymbolsForType(type: ConeKotlinType, useSiteSession: FirSession): List<FirClassSymbol<*>> {
    val lookupTags = mutableListOf<ConeClassLikeLookupTag>()

    fun ConeKotlinType.collectClassIds() {
        when (val unwrappedType = unwrapToSimpleTypeUsingLowerBound().fullyExpandedType(useSiteSession)) {
            is ConeClassLikeType -> lookupTags.addIfNotNull(unwrappedType.lookupTag)
            is ConeIntersectionType -> unwrappedType.intersectedTypes.forEach { it.collectClassIds() }
            else -> {}
        }
    }

    type.collectClassIds()
    return lookupTags.mapNotNull { it.toClassSymbol(useSiteSession) }
}

fun lookupSuperTypes(
    symbols: List<FirClassSymbol<*>>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    substituteTypes: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>> = SmartSet.create(),
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        for (symbol in symbols) {
            symbol.collectSuperTypes(it, visitedSymbols, deep, lookupInterfaces, substituteTypes, useSiteSession, supertypeSupplier)
        }
    }
}

fun lookupSuperTypes(
    klass: FirClass,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    substituteTypes: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        klass.symbol.collectSuperTypes(it, SmartSet.create(), deep, lookupInterfaces, substituteTypes, useSiteSession, supertypeSupplier)
    }
}

fun FirClassSymbol<*>.isSubclassOf(
    ownerLookupTag: ConeClassLikeLookupTag,
    session: FirSession,
    isStrict: Boolean,
    lookupInterfaces: Boolean
): Boolean {
    lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
    return fir.isSubclassOf(ownerLookupTag, session, isStrict, SupertypeSupplier.Default, lookupInterfaces)
}

fun FirClass.isSubclassOf(
    ownerLookupTag: ConeClassLikeLookupTag,
    session: FirSession,
    isStrict: Boolean,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    lookupInterfaces: Boolean = true,
): Boolean {
    if (symbol.toLookupTag() == ownerLookupTag) {
        return !isStrict
    }

    return lookupSuperTypes(
        this,
        lookupInterfaces = lookupInterfaces,
        deep = true,
        session,
        substituteTypes = false,
        supertypeSupplier
    ).any { superType ->
        // Note: We just check lookupTag here, so type substitution isn't needed
        superType.lookupTag == ownerLookupTag
    }
}

fun FirClass.isThereLoopInSupertypes(session: FirSession): Boolean {
    val visitedSymbols: MutableSet<FirClassifierSymbol<*>> = SmartSet.create()
    val inProcess: MutableSet<FirClassifierSymbol<*>> = mutableSetOf()

    var isThereLoop = false

    fun dfs(current: FirClassifierSymbol<*>) {
        if (current in visitedSymbols) return
        if (!inProcess.add(current)) {
            isThereLoop = true
            return
        }

        when (val fir = current.fir) {
            is FirClass -> {
                fir.superConeTypes.forEach {
                    it.lookupTag.toSymbol(session)?.let(::dfs)
                }
            }
            is FirTypeAlias -> {
                fir.expandedConeType?.lookupTag?.toSymbol(session)?.let(::dfs)
            }
            else -> {}
        }

        visitedSymbols.add(current)
        inProcess.remove(current)
    }

    dfs(symbol)

    return isThereLoop
}

fun lookupSuperTypes(
    symbol: FirClassLikeSymbol<*>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        symbol.collectSuperTypes(it, SmartSet.create(), deep, lookupInterfaces, false, useSiteSession, SupertypeSupplier.Default)
    }
}

inline fun <reified ID : Any, reified FS : FirScope> scopeSessionKey(): ScopeSessionKey<ID, FS> {
    return object : ScopeSessionKey<ID, FS>() {}
}

val USE_SITE: ScopeSessionKey<Pair<FirSession, FirClassSymbol<*>>, FirTypeScope> = scopeSessionKey()

/* TODO REMOVE */
fun createSubstitutionForScope(
    typeParameters: List<FirTypeParameterRef>, // TODO: or really declared?
    type: ConeClassLikeType,
    session: FirSession
): Map<FirTypeParameterSymbol, ConeKotlinType> {
    val capturedOrType = session.typeContext.captureFromArguments(type, CaptureStatus.FROM_EXPRESSION) ?: type
    val capturedTypeArguments = capturedOrType.asCone().typeArguments

    return typeParameters.withIndex().mapNotNull { (index, typeParameter) ->
        val capturedTypeArgument = capturedTypeArguments.getOrNull(index) ?: return@mapNotNull null
        require(capturedTypeArgument is ConeKotlinType) {
            "There should left no projections after capture conversion, but $capturedTypeArgument found at $index"
        }
        val originalTypeArgument = type.typeArguments.getOrNull(index) ?: return@mapNotNull null

        val typeParameterSymbol = typeParameter.symbol
        val resultingArgument =
            computeNonTrivialTypeArgumentForScopeSubstitutor(typeParameterSymbol, originalTypeArgument, session, capturedTypeArgument)
                ?: capturedTypeArgument

        typeParameterSymbol to resultingArgument
    }.toMap()
}

/**
 * Returns null if `capturedTypeArgument` should be used
 */
private fun computeNonTrivialTypeArgumentForScopeSubstitutor(
    typeParameterSymbol: FirTypeParameterSymbol,
    originalTypeArgument: ConeTypeProjection,
    session: FirSession,
    capturedTypeArgument: ConeKotlinType
): ConeKotlinType? {
    // We don't do anything for contravariant parameters (IN), because their UnsafeVariance usages are mostly return type.
    // And if we continue using captured types for them, they will just be approximated (as return types) as they've been before.
    if (typeParameterSymbol.variance != Variance.OUT_VARIANCE) return null

    return when (originalTypeArgument.kind) {
        // Out<out T> is the same as Out<T>
        ProjectionKind.OUT -> originalTypeArgument.type!!
        // Out<*> is the same as Out<SubstitutedUpperBounds> (i.e. Out<Supertype(CapturedType(*))>)
        ProjectionKind.STAR -> session.typeApproximator.approximateToSuperType(
            capturedTypeArgument, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        )
        else -> null
    }
}

fun ConeClassLikeType.computePartialExpansion(
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
): ConeClassLikeType = fullyExpandedType(useSiteSession) { supertypeSupplier.expansionForTypeAlias(it, useSiteSession) }

private fun FirClassLikeSymbol<*>.collectSuperTypes(
    list: MutableList<ConeClassLikeType>,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
): Unit = forEachSupertype(
    visitedSymbols, deep, lookupInterfaces, substituteSuperTypes,
    useSiteSession, supertypeSupplier,
    onType = { list += it },
)

private inline fun FirClassLikeSymbol<*>.forEachSupertype(
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier,
    onType: (ConeClassLikeType) -> Unit,
): Unit = forEachSupertypeWithInheritor(
    deep, lookupInterfaces, substituteSuperTypes,
    useSiteSession, supertypeSupplier,
    visitedSymbols = visitedSymbols,
    onSupertypeAndInheritor = { it, _ -> onType(it) },
)

inline fun FirClassLikeSymbol<*>.forEachSupertypeWithInheritor(
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>> = mutableSetOf(),
    onSupertypeAndInheritor: (ConeClassLikeType, FirClassLikeSymbol<*>) -> Unit,
) {
    val substitutor: ConeSubstitutor = ConeSubstitutor.Empty

    (this to substitutor).traverseDepthFirstWithoutDuplicates(
        getSubsequent = act@{ (next, substitutor) ->
            return@act when (next) {
                is FirClassSymbol<*> -> {
                    val superClassTypes =
                        supertypeSupplier.forClass(next.fir, useSiteSession).mapNotNull {
                            it.computePartialExpansion(useSiteSession, supertypeSupplier)
                                .takeIf { type -> lookupInterfaces || type.isClassBasedType(useSiteSession) }
                        }
                    superClassTypes.forEach {
                        onSupertypeAndInheritor(substitutor.substituteOrSelf(it) as ConeClassLikeType, next)
                    }
                    when {
                        deep -> superClassTypes.mapNotNull {
                            if (it is ConeErrorType) return@mapNotNull null
                            it to when {
                                substituteSuperTypes -> createSubstitutionForSupertype(it, useSiteSession).chain(substitutor)
                                else -> substitutor
                            }
                        }
                        else -> emptyList()
                    }
                }
                is FirTypeAliasSymbol -> {
                    val expansion = supertypeSupplier
                        .expansionForTypeAlias(next.fir, useSiteSession)
                        ?.computePartialExpansion(useSiteSession, supertypeSupplier)
                        ?: return@act emptyList()
                    listOf(expansion to substitutor)
                }
            }
        },
        toStackElement = { _, (it, substitutor) -> it.lookupTag.toSymbol(useSiteSession)?.to(substitutor) },
        visit = { (symbol, _) -> visitedSymbols.add(symbol) },
    )
}

inline fun <T, K> T.traverseDepthFirstWithoutDuplicates(
    getSubsequent: (T) -> List<K>,
    toStackElement: (T, K) -> T?,
    visit: (T) -> Boolean,
) {
    val stack = mutableListOf(this)

    while (stack.isNotEmpty()) {
        val current = stack.popLast()

        if (visit(current)) {
            getSubsequent(current).forEach { next ->
                toStackElement(current, next)?.let(stack::add)
            }
        }
    }
}

fun ConeClassLikeType?.isClassBasedType(
    useSiteSession: FirSession
): Boolean {
    if (this is ConeErrorType) return false
    val symbol = this?.lookupTag?.toClassSymbol(useSiteSession) ?: return false
    return when (symbol) {
        is FirAnonymousObjectSymbol -> true
        is FirRegularClassSymbol -> symbol.fir.classKind == ClassKind.CLASS
    }
}

fun createSubstitutionForSupertype(superType: ConeLookupTagBasedType, session: FirSession): ConeSubstitutor {
    val klass = superType.lookupTag.toRegularClassSymbol(session)?.fir ?: return ConeSubstitutor.Empty
    val arguments = superType.typeArguments.map {
        it as? ConeKotlinType ?: ConeErrorType(ConeSimpleDiagnostic("illegal projection usage", DiagnosticKind.IllegalProjectionUsage))
    }
    val mapping = klass.typeParameters.map { it.symbol }.zip(arguments).toMap()
    return substitutorByMap(mapping, session)
}

/**
 * @return `null` only if symbol for `kotlin.Any` is not found (in no-runtime environments)
 */
fun FirRegularClassSymbol.getSuperClassSymbolOrAny(session: FirSession): FirRegularClassSymbol? {
    for (superType in resolvedSuperTypes) {
        val symbol = superType.fullyExpandedType(session).toRegularClassSymbol(session) ?: continue
        if (symbol.classKind == ClassKind.CLASS) return symbol
    }
    return session.builtinTypes.anyType.coneType.toRegularClassSymbol(session)
}

fun FirClassLikeSymbol<*>.getSuperTypes(
    useSiteSession: FirSession,
    recursive: Boolean = true,
    lookupInterfaces: Boolean = true,
    substituteSuperTypes: Boolean = true,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        collectSuperTypes(it, SmartSet.create(), recursive, lookupInterfaces, substituteSuperTypes, useSiteSession, supertypeSupplier)
    }
}

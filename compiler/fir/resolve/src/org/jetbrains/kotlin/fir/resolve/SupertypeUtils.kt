/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.transformers.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet

abstract class SupertypeSupplier {
    abstract fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType>
    abstract fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType?

    object Default : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            if (!firClass.isLocal) {
                // for local classes the phase may not be updated till that moment
                firClass.ensureResolved(FirResolvePhase.SUPER_TYPES)
            }
            return firClass.superConeTypes
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            typeAlias.ensureResolved(FirResolvePhase.SUPER_TYPES)
            return typeAlias.expandedConeType
        }
    }
}

fun lookupSuperTypes(
    klass: FirClass,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    substituteTypes: Boolean = false
): List<ConeClassLikeType> {
    return lookupSuperTypes(klass.symbol, lookupInterfaces, deep, useSiteSession, supertypeSupplier, substituteTypes)
}

fun lookupSuperTypes(
    classSymbol: FirClassSymbol<*>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    substituteTypes: Boolean = false
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        classSymbol.collectSuperTypes(it, SmartSet.create(), deep, lookupInterfaces, substituteTypes, useSiteSession, supertypeSupplier)
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
        }

        visitedSymbols.add(current)
        inProcess.remove(current)
    }

    dfs(symbol)

    return isThereLoop
}

fun lookupSuperTypes(
    symbol: FirClassifierSymbol<*>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default
): List<ConeClassLikeType> {
    return SmartList<ConeClassLikeType>().also {
        symbol.collectSuperTypes(it, SmartSet.create(), deep, lookupInterfaces, false, useSiteSession, supertypeSupplier)
    }
}

inline fun <reified ID : Any, reified FS : FirScope> scopeSessionKey(): ScopeSessionKey<ID, FS> {
    return object : ScopeSessionKey<ID, FS>() {}
}

val USE_SITE = scopeSessionKey<FirClassSymbol<*>, FirTypeScope>()

/* TODO REMOVE */
fun createSubstitution(
    typeParameters: List<FirTypeParameterRef>, // TODO: or really declared?
    type: ConeClassLikeType,
    session: FirSession
): Map<FirTypeParameterSymbol, ConeKotlinType> {
    val capturedOrType = session.typeContext.captureFromArguments(type, CaptureStatus.FROM_EXPRESSION) ?: type
    val typeArguments = (capturedOrType as ConeClassLikeType).typeArguments
    return typeParameters.zip(typeArguments) { typeParameter, typeArgument ->
        val typeParameterSymbol = typeParameter.symbol
        typeParameterSymbol to when (typeArgument) {
            is ConeKotlinTypeProjection -> {
                typeArgument.type
            }
            else /* StarProjection */ -> {
                ConeTypeIntersector.intersectTypes(
                    session.typeContext,
                    typeParameterSymbol.fir.bounds.map { it.coneType }
                )
            }
        }
    }.toMap()
}

private fun ConeClassLikeType.computePartialExpansion(
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
): ConeClassLikeType = fullyExpandedType(useSiteSession) { supertypeSupplier.expansionForTypeAlias(it, useSiteSession) }

private fun FirClassifierSymbol<*>.collectSuperTypes(
    list: MutableList<ConeClassLikeType>,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    substituteSuperTypes: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
) {
    if (!visitedSymbols.add(this)) return
    when (this) {
        is FirClassSymbol<*> -> {
            val superClassTypes =
                supertypeSupplier.forClass(fir, useSiteSession).mapNotNull {
                    it.computePartialExpansion(useSiteSession, supertypeSupplier)
                        .takeIf { type -> lookupInterfaces || type.isClassBasedType(useSiteSession) }
                }
            list += superClassTypes
            if (deep)
                superClassTypes.forEach {
                    if (it !is ConeClassErrorType) {
                        if (substituteSuperTypes) {
                            val substitutedTypes = SmartList<ConeClassLikeType>()
                            it.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(
                                substitutedTypes,
                                visitedSymbols,
                                deep,
                                lookupInterfaces,
                                substituteSuperTypes,
                                useSiteSession,
                                supertypeSupplier
                            )
                            val substitutor = createSubstitutionForSupertype(it, useSiteSession)
                            substitutedTypes.mapTo(list) { superType -> substitutor.substituteOrSelf(superType) as ConeClassLikeType }
                        } else {
                            it.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(
                                list,
                                visitedSymbols,
                                deep,
                                lookupInterfaces,
                                substituteSuperTypes,
                                useSiteSession,
                                supertypeSupplier
                            )
                        }
                    }
                }
        }
        is FirTypeAliasSymbol -> {
            val expansion = supertypeSupplier
                .expansionForTypeAlias(fir, useSiteSession)
                ?.computePartialExpansion(useSiteSession, supertypeSupplier)
                ?: return
            expansion.lookupTag.toSymbol(useSiteSession)
                ?.collectSuperTypes(list, visitedSymbols, deep, lookupInterfaces, substituteSuperTypes, useSiteSession, supertypeSupplier)
        }
        else -> error("?!id:1")
    }
}

private fun ConeClassLikeType?.isClassBasedType(
    useSiteSession: FirSession
): Boolean {
    if (this is ConeClassErrorType) return false
    val symbol = this?.lookupTag?.toSymbol(useSiteSession) as? FirClassSymbol ?: return false
    return when (symbol) {
        is FirAnonymousObjectSymbol -> true
        is FirRegularClassSymbol -> symbol.fir.classKind == ClassKind.CLASS
    }
}

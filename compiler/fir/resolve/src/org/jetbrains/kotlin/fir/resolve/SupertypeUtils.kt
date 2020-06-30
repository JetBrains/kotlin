/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

abstract class SupertypeSupplier {
    abstract fun forClass(firClass: FirClass<*>): List<ConeClassLikeType>
    abstract fun expansionForTypeAlias(typeAlias: FirTypeAlias): ConeClassLikeType?

    object Default : SupertypeSupplier() {
        override fun forClass(firClass: FirClass<*>) = firClass.superConeTypes
        override fun expansionForTypeAlias(typeAlias: FirTypeAlias) = typeAlias.expandedConeType
    }
}

fun lookupSuperTypes(
    klass: FirClass<*>,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default
): List<ConeClassLikeType> {
    return mutableListOf<ConeClassLikeType>().also {
        klass.symbol.collectSuperTypes(it, mutableSetOf(), deep, lookupInterfaces, useSiteSession, supertypeSupplier)
    }
}

inline fun <reified ID : Any, reified FS : FirScope> scopeSessionKey(): ScopeSessionKey<ID, FS> {
    return object : ScopeSessionKey<ID, FS>() {}
}

val USE_SITE = scopeSessionKey<FirClassSymbol<*>, FirTypeScope>()

data class SubstitutionScopeKey(val type: ConeClassLikeType) : ScopeSessionKey<FirClassLikeSymbol<*>, FirClassSubstitutionScope>()

fun FirClass<*>.buildUseSiteMemberScope(useSiteSession: FirSession, builder: ScopeSession): FirScope? {
    return this.unsubstitutedScope(useSiteSession, builder)
}

/* TODO REMOVE */
fun createSubstitution(
    typeParameters: List<FirTypeParameterRef>, // TODO: or really declared?
    typeArguments: Array<out ConeTypeProjection>,
    session: FirSession
): Map<FirTypeParameterSymbol, ConeKotlinType> {
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

fun ConeClassLikeType.wrapSubstitutionScopeIfNeed(
    session: FirSession,
    useSiteMemberScope: FirTypeScope,
    declaration: FirClassLikeDeclaration<*>,
    builder: ScopeSession,
    derivedClassId: ClassId? = null
): FirTypeScope {
    if (this.typeArguments.isEmpty()) return useSiteMemberScope
    return builder.getOrBuild(declaration.symbol, SubstitutionScopeKey(this)) {
        val typeParameters = (declaration as? FirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        val originalSubstitution = createSubstitution(typeParameters, typeArguments, session)
        val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(declaration.symbol.classId.asSingleFqName().toUnsafe())
        val javaClass = javaClassId?.let { session.firSymbolProvider.getClassLikeSymbolByFqName(it)?.fir } as? FirRegularClass
        if (javaClass != null) {
            // This kind of substitution is necessary when method which is mapped from Java (e.g. Java Map.forEach)
            // is called on an external type, like MyMap<String, String>,
            // to determine parameter types properly (e.g. String, String instead of K, V)
            val javaTypeParameters = javaClass.typeParameters
            val javaSubstitution = createSubstitution(javaTypeParameters, typeArguments, session)
            FirClassSubstitutionScope(
                session, useSiteMemberScope, builder, originalSubstitution + javaSubstitution,
                skipPrivateMembers = true, derivedClassId = derivedClassId
            )
        } else {
            FirClassSubstitutionScope(
                session, useSiteMemberScope, builder, originalSubstitution,
                skipPrivateMembers = true, derivedClassId = derivedClassId
            )
        }
    }
}

private fun ConeClassLikeType.computePartialExpansion(
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
): ConeClassLikeType = fullyExpandedType(useSiteSession, supertypeSupplier::expansionForTypeAlias)

private fun FirClassifierSymbol<*>.collectSuperTypes(
    list: MutableList<ConeClassLikeType>,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    deep: Boolean,
    lookupInterfaces: Boolean,
    useSiteSession: FirSession,
    supertypeSupplier: SupertypeSupplier
) {
    if (!visitedSymbols.add(this)) return
    when (this) {
        is FirClassSymbol<*> -> {
            val superClassTypes =
                supertypeSupplier.forClass(fir).mapNotNull {
                    it.computePartialExpansion(useSiteSession, supertypeSupplier)
                        .takeIf { type -> lookupInterfaces || type.isClassBasedType(useSiteSession) }
                }
            list += superClassTypes
            if (deep)
                superClassTypes.forEach {
                    if (it !is ConeClassErrorType) {
                        it.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(
                            list,
                            visitedSymbols,
                            deep,
                            lookupInterfaces,
                            useSiteSession,
                            supertypeSupplier
                        )
                    }
                }
        }
        is FirTypeAliasSymbol -> {
            val expansion =
                supertypeSupplier.expansionForTypeAlias(fir)?.computePartialExpansion(useSiteSession, supertypeSupplier) ?: return
            expansion.lookupTag.toSymbol(useSiteSession)
                ?.collectSuperTypes(list, visitedSymbols, deep, lookupInterfaces, useSiteSession, supertypeSupplier)
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

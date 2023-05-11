/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.mpp.ClassLikeSymbolMarker
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.mpp.TypeAliasSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

sealed class FirClassLikeSymbol<D : FirClassLikeDeclaration>(
    val classId: ClassId,
) : FirClassifierSymbol<D>(), ClassLikeSymbolMarker {
    abstract override fun toLookupTag(): ConeClassLikeLookupTag

    val name get() = classId.shortClassName

    fun getDeprecation(apiVersion: ApiVersion): DeprecationsPerUseSite? {
        if (annotations.isEmpty()) return null
        lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        return fir.deprecationsProvider.getDeprecationsInfo(apiVersion)
    }

    val rawStatus: FirDeclarationStatus
        get() = fir.status

    val resolvedStatus: FirResolvedDeclarationStatus
        get() = fir.resolvedStatus()

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }

    override fun toString(): String = "${this::class.simpleName} ${classId.asString()}"
}

sealed class FirClassSymbol<C : FirClass>(classId: ClassId) : FirClassLikeSymbol<C>(classId) {
    private val lookupTag: ConeClassLikeLookupTag =
        if (classId.isLocal) ConeClassLookupTagWithFixedSymbol(classId, this)
        else classId.toLookupTag()

    override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag

    val resolvedSuperTypeRefs: List<FirResolvedTypeRef>
        get() {
            lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            @Suppress("UNCHECKED_CAST")
            return fir.superTypeRefs as List<FirResolvedTypeRef>
        }

    val resolvedSuperTypes: List<ConeKotlinType>
        get() = resolvedSuperTypeRefs.map { it.coneType }

    val declarationSymbols: List<FirBasedSymbol<*>>
        get() = fir.declarations.map { it.symbol }

    val classKind: ClassKind
        get() = fir.classKind
}

class FirRegularClassSymbol(classId: ClassId) : FirClassSymbol<FirRegularClass>(classId), RegularClassSymbolMarker {
    val companionObjectSymbol: FirRegularClassSymbol?
        get() = fir.companionObjectSymbol

    val resolvedContextReceivers: List<FirContextReceiver>
        get() {
            if (fir.contextReceivers.isEmpty()) return emptyList()
            lazyResolveToPhase(FirResolvePhase.TYPES)
            return fir.contextReceivers
        }
}

val ANONYMOUS_CLASS_ID = ClassId(FqName.ROOT, FqName.topLevel(SpecialNames.ANONYMOUS), true)

class FirAnonymousObjectSymbol : FirClassSymbol<FirAnonymousObject>(ANONYMOUS_CLASS_ID)

class FirTypeAliasSymbol(classId: ClassId) : FirClassLikeSymbol<FirTypeAlias>(classId), TypeAliasSymbolMarker {
    override fun toLookupTag(): ConeClassLikeLookupTag = classId.toLookupTag()

    val resolvedExpandedTypeRef: FirResolvedTypeRef
        get() {
            lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            return fir.expandedTypeRef as FirResolvedTypeRef
        }
}

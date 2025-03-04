/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.mpp.ClassLikeSymbolMarker
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.mpp.TypeAliasSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

sealed class FirClassLikeSymbol<out D : FirClassLikeDeclaration>(
    val classId: ClassId,
) : FirClassifierSymbol<D>(), ClassLikeSymbolMarker {
    abstract override fun toLookupTag(): ConeClassLikeLookupTag

    val name: Name get() = classId.shortClassName

    fun getOwnDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        if (deprecationsAreDefinitelyEmpty()) {
            // here should probably be `null`, see KT-74133
            return EmptyDeprecationsPerUseSite
        }

        lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        return fir.deprecationsProvider.getDeprecationsInfo(languageVersionSettings)
    }

    private fun deprecationsAreDefinitelyEmpty(): Boolean {
        if (origin is FirDeclarationOrigin.Java) {
            // Java may perform lazy resolution when accessing FIR tree internals, see KT-55387
            return false
        }
        if (annotations.isEmpty() && fir.versionRequirements.isNullOrEmpty()) return true
        if (fir.deprecationsProvider == EmptyDeprecationsProvider) return true
        return false
    }

    val rawStatus: FirDeclarationStatus
        get() = fir.status

    val resolvedStatus: FirResolvedDeclarationStatus
        get() = fir.resolvedStatus()

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }

    val ownTypeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.mapNotNull { (it as? FirTypeParameter)?.symbol }

    override fun toString(): String = "${this::class.simpleName} ${classId.asString()}"
}

sealed class FirClassSymbol<out C : FirClass>(classId: ClassId) : FirClassLikeSymbol<C>(classId) {
    private val lookupTag: ConeClassLikeLookupTag =
        if (classId.isLocal) ConeClassLikeLookupTagWithFixedSymbol(classId, this)
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

    @DirectDeclarationsAccess
    val declarationSymbols: List<FirBasedSymbol<*>>
        get() = fir.declarations.map { it.symbol }

    val classKind: ClassKind
        get() = fir.classKind
}

class FirRegularClassSymbol(classId: ClassId) : FirClassSymbol<FirRegularClass>(classId), RegularClassSymbolMarker {
    val companionObjectSymbol: FirRegularClassSymbol?
        get() = fir.companionObjectSymbol

    val resolvedContextParameters: List<FirValueParameter>
        get() {
            if (fir.contextParameters.isEmpty()) return emptyList()
            lazyResolveToPhase(FirResolvePhase.TYPES)
            return fir.contextParameters
        }
}

class FirAnonymousObjectSymbol(packageFqName: FqName) : FirClassSymbol<FirAnonymousObject>(
    ClassId(packageFqName, SpecialNames.ANONYMOUS_FQ_NAME, isLocal = true)
)

class FirTypeAliasSymbol(classId: ClassId) : FirClassLikeSymbol<FirTypeAlias>(classId), TypeAliasSymbolMarker {
    private val lookupTag: ConeClassLikeLookupTag =
        if (classId.isLocal) ConeClassLikeLookupTagWithFixedSymbol(classId, this)
        else classId.toLookupTag()

    override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag


    val resolvedExpandedTypeRef: FirResolvedTypeRef
        get() {
            lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            return fir.expandedTypeRef as FirResolvedTypeRef
        }
}

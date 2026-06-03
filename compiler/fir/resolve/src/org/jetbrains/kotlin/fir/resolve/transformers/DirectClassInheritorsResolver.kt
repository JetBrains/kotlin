/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.addDirectInheritors
import org.jetbrains.kotlin.fir.declarations.directInheritors
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassIdFromDependencies
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.name.ClassId

class DirectClassInheritorsResolver(override val session: FirSession) : SessionHolder {

    private fun collectInheritorsOfCorrespondingExpectClass(expectClassId: ClassId, expansionClass: FirRegularClass) {
        if (LanguageFeature.MultiPlatformProjects.isDisabled()) return
        val correspondingExpectClass = session.getRegularClassSymbolByClassIdFromDependencies(expectClassId)?.fir ?: return
        if (correspondingExpectClass.isExpect) {
            expansionClass.addDirectInheritors(correspondingExpectClass.directInheritors)
        }
    }

    private fun extractClassFromTypeRef(typeRef: FirTypeRef): FirRegularClass? {
        val lookupTag = typeRef.coneType.lookupTagIfAny ?: return null
        val classLikeSymbol: FirClassifierSymbol<*> = lookupTag.toSymbol(session) ?: return null
        return when (classLikeSymbol) {
            is FirRegularClassSymbol -> classLikeSymbol.fir
            is FirTypeAliasSymbol -> {
                classLikeSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                extractClassFromTypeRef(classLikeSymbol.fir.expandedTypeRef)
            }
            else -> null
        }
    }

    fun resolveRegularClass(regularClass: FirRegularClass) {
        val symbol = regularClass.symbol
        for (typeRef in regularClass.superTypeRefs) {
            val parent = extractClassFromTypeRef(typeRef) ?: continue
            parent.addDirectInheritors(symbol)
        }

        if (!symbol.isLocal) collectInheritorsOfCorrespondingExpectClass(symbol.classId, regularClass)
    }

    fun resolveTypeAlias(typeAlias: FirTypeAlias) {
        if (!typeAlias.isActual || typeAlias.isLocal) return
        val expansionClass = typeAlias.expandedTypeRef.coneType.toRegularClassSymbol(session)?.fir ?: return
        collectInheritorsOfCorrespondingExpectClass(typeAlias.classId, expansionClass)
    }

    fun resolveAnonymousObject(anonymousObject: FirAnonymousObject) {
        val symbol = anonymousObject.symbol
        for (typeRef in anonymousObject.superTypeRefs) {
            val parent = extractClassFromTypeRef(typeRef) ?: continue
            parent.addDirectInheritors(symbol)
        }
    }
}

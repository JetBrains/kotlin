/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.imports

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.imports.KaExplicitImport
import org.jetbrains.kotlin.analysis.api.imports.KaStarImport
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtImportDirective

internal class KaFirExplicitImport(
    private val firImport: FirResolvedImport,
    private val builder: KaSymbolByFirBuilder,
    private val analysisSession: KaFirSession,
) : KaExplicitImport {
    override val token: KaLifetimeToken get() = builder.token

    override val psi: KtImportDirective?
        get() = withValidityAssertion { firImport.source?.psi as? KtImportDirective }

    override val importedFqName by cached { firImport.importedFqName }

    override val aliasName by cached { firImport.aliasName }

    override val importedName by cached { firImport.aliasName ?: firImport.importedName }

    override val classifierSymbol: KaClassifierSymbol? by cached {
        val importedName = firImport.importedName ?: return@cached null
        val parentClassId = firImport.resolvedParentClassId
        val classId = parentClassId?.createNestedClassId(importedName)
            ?: ClassId.topLevel(firImport.packageFqName.child(importedName))
        val firSymbol = analysisSession.firSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return@cached null
        builder.classifierBuilder.buildClassLikeSymbol(firSymbol)
    }

    @OptIn(FirSymbolProviderInternals::class)
    override val callableSymbols: List<KaCallableSymbol> by cached {
        val importedName = firImport.importedName ?: return@cached emptyList()

        val firSymbols = mutableListOf<FirCallableSymbol<*>>()
        val session = analysisSession.firSession
        val parentClassId = firImport.resolvedParentClassId

        if (parentClassId != null) {
            // Static member of an object or class.
            val parentClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)
                ?.fullyExpandedClass(session)
            if (parentClassSymbol != null) {
                val scopeSession = analysisSession.getScopeSessionFor(session)
                val staticsScope = parentClassSymbol.fir.scopeProvider
                    .getStaticScope(parentClassSymbol.fir, session, scopeSession)
                staticsScope?.processFunctionsByName(importedName) { firSymbols += it }
                staticsScope?.processPropertiesByName(importedName) { firSymbols += it }
            }
        } else {
            session.symbolProvider.getTopLevelCallableSymbolsTo(firSymbols, firImport.packageFqName, importedName)
        }

        firSymbols.map { builder.callableBuilder.buildCallableSymbol(it) }
    }
}

internal class KaFirStarImport(
    private val firImport: FirResolvedImport,
    private val builder: KaSymbolByFirBuilder,
    private val analysisSession: KaFirSession,
) : KaStarImport {
    override val token: KaLifetimeToken get() = builder.token

    override val psi: KtImportDirective?
        get() = withValidityAssertion { firImport.source?.psi as? KtImportDirective }

    override val importedFqName by cached { firImport.importedFqName }

    override val classifierSymbol: KaClassifierSymbol? by cached {
        val classId = firImport.resolvedParentClassId ?: return@cached null
        val firSymbol = analysisSession.firSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return@cached null
        builder.classifierBuilder.buildClassLikeSymbol(firSymbol)
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KtFirSymbolContainingDeclarationProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtSymbolContainingDeclarationProvider(), KtFirAnalysisSessionComponent {
    override fun getContainingDeclaration(symbol: KtSymbol): KtDeclarationSymbol? {
        if (symbol is KtReceiverParameterSymbol) {
            return symbol.owningCallableSymbol
        }

        if (symbol !is KtDeclarationSymbol) return null
        if (symbol is KtSymbolWithKind &&
            symbol.symbolKind == KtSymbolKind.TOP_LEVEL &&
            // Should be replaced with proper check after KT-61451 and KT-61887
            (symbol.firSymbol.fir as? FirElementWithResolveState)?.getContainingFile()?.declarations?.firstOrNull() !is FirScript
        ) return null

        val firSymbol = symbol.firSymbol
        if (firSymbol is FirErrorPropertySymbol && firSymbol.diagnostic is ConeDestructuringDeclarationsOnTopLevel) return null
        fun getParentSymbolByPsi() = getContainingPsi(symbol).let { with(analysisSession) { it.getSymbol() } }
        return when (symbol) {
            is KtPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.fir.propertySymbol) as? KtDeclarationSymbol
            is KtBackingFieldSymbol -> symbol.owningProperty
            is KtTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KtDeclarationSymbol
            is KtLocalVariableSymbol -> getParentSymbolByPsi()
            is KtAnonymousFunctionSymbol -> getParentSymbolByPsi()
            is KtAnonymousObjectSymbol -> getParentSymbolByPsi()
            is KtDestructuringDeclarationSymbol -> getParentSymbolByPsi()

            is KtSamConstructorSymbol -> null // SAM constructors are always top-level
            is KtScriptSymbol -> null // Scripts are always top-level

            is KtClassInitializerSymbol -> {
                val outerFirClassifier = symbol.firSymbol.getContainingClassSymbol(symbol.firSymbol.llFirSession)
                    ?: return getParentSymbolByPsi()
                firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
            }

            is KtValueParameterSymbol -> {
                firSymbolBuilder.callableBuilder.buildCallableSymbol(symbol.firSymbol.fir.containingFunctionSymbol)
            }

            is KtCallableSymbol -> {
                val outerFirClassifier = symbol.firSymbol.getContainingClassSymbol(symbol.firSymbol.llFirSession)
                if (outerFirClassifier == null) {
                    return when (firSymbol.origin) {
                        FirDeclarationOrigin.DynamicScope -> {
                            // A callable declaration from dynamic scope has no containing declaration as it comes from a dynamic type
                            // which is not based on a specific classifier
                            null
                        }
                        else -> getParentSymbolByPsi()
                    }
                }
                firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
            }

            is KtClassLikeSymbol -> {
                val classId = symbol.classIdIfNonLocal ?: return getParentSymbolByPsi() // local
                val outerClassId = classId.outerClassId ?: return getParentSymbolByPsi() // top-level or inside script
                val outerFirClassifier = symbol.firSymbol.llFirSession.firProvider.getFirClassifierByFqName(outerClassId) ?: return null
                firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
            }
        }
    }

    override fun getContainingModule(symbol: KtSymbol): KtModule {
        return symbol.getContainingKtModule(analysisSession.firResolveSession)
    }


    private fun getContainingPsi(symbol: KtSymbol): KtDeclaration {
        val source = symbol.firSymbol.source
        val thisSource = when (source?.kind) {
            null -> errorWithAttachment("PSI should present for declaration built by Kotlin code") {
                withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
            }

            KtFakeSourceElementKind.ImplicitConstructor -> return source.psi as KtDeclaration
            KtFakeSourceElementKind.PropertyFromParameter -> return source.psi?.parentOfType<KtPrimaryConstructor>()!!
            KtFakeSourceElementKind.EnumInitializer -> return source.psi as KtEnumEntry
            KtFakeSourceElementKind.ScriptParameter -> return source.psi as KtScript
            KtRealSourceElementKind -> source.psi!!
            else ->
                errorWithAttachment("errorWithAttachment FirSourceElement: kind=${source.kind} element=${source.psi!!::class.simpleName}") {
                    withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
                }
        }

        val origin = symbol.origin
        return when {
            origin == KtSymbolOrigin.SOURCE || symbol.firSymbol.fir.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> thisSource.getContainingKtDeclaration()
                ?: errorWithAttachment("Containing declaration should present for non-toplevel declaration ${thisSource::class}") {
                    withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
                }

            origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> thisSource as KtDeclaration
            else -> errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${thisSource::class}") {
                withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
            }
        }
    }

    private fun PsiElement.getContainingKtDeclaration(): KtDeclaration? =
        when (val container = this.parentOfType<KtDeclaration>()) {
            is KtDestructuringDeclaration -> container.parentOfType()
            else -> container
        }?.let { it.originalDeclaration ?: it }
}

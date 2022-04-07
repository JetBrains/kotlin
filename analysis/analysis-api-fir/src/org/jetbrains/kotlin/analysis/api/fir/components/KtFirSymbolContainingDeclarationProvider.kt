/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.impl.barebone.parentOfType
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*

internal class KtFirSymbolContainingDeclarationProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolContainingDeclarationProvider(), KtFirAnalysisSessionComponent {
    override fun getContainingDeclaration(symbol: KtSymbol): KtSymbolWithKind? {
        assertIsValidAndAccessible()

        if (symbol is KtReceiverParameterSymbol) {
            return firSymbolBuilder.buildSymbol((symbol as KtFirReceiverParameterSymbol).firSymbol) as KtSymbolWithKind
        }

        if (symbol is KtPackageSymbol) return null
        if (symbol is KtSymbolWithKind && symbol.symbolKind == KtSymbolKind.TOP_LEVEL) return null
        if (symbol is KtCallableSymbol) {
            val classId = symbol.callableIdIfNonLocal?.classId
            if (classId != null) {
                with(analysisSession) {
                    return classId.getCorrespondingToplevelClassOrObjectSymbol()
                }
            }
        }
        return when (symbol) {
            is KtFirTypeParameterSymbol -> {
                firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as KtSymbolWithKind
            }
            is KtSymbolWithKind -> when (symbol.origin) {
                KtSymbolOrigin.SOURCE, KtSymbolOrigin.SOURCE_MEMBER_GENERATED ->
                    getContainingDeclarationForKotlinInSourceSymbol(symbol)
                KtSymbolOrigin.LIBRARY, KtSymbolOrigin.JAVA, KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY ->
                    getContainingDeclarationForLibrarySymbol(symbol)
                KtSymbolOrigin.PROPERTY_BACKING_FIELD -> getContainingDeclarationForBackingFieldSymbol(symbol)
                KtSymbolOrigin.INTERSECTION_OVERRIDE -> TODO()
                KtSymbolOrigin.SAM_CONSTRUCTOR -> null
                KtSymbolOrigin.PLUGIN -> TODO("Containing declaration is requested for ${ DebugSymbolRenderer.render(symbol) }")
                KtSymbolOrigin.DELEGATED -> TODO()
            }
            else -> null
        }
    }

    override fun getContainingModule(symbol: KtSymbol): KtModule {
       return symbol.getContainingKtModule(analysisSession.firResolveState)
    }

    private fun getContainingDeclarationForBackingFieldSymbol(symbol: KtSymbolWithKind): KtSymbolWithKind {
        require(symbol is KtBackingFieldSymbol)
        return symbol.owningProperty
    }

    private fun getContainingDeclarationForKotlinInSourceSymbol(symbol: KtSymbolWithKind): KtSymbolWithKind = with(analysisSession) {
        require(symbol.origin == KtSymbolOrigin.SOURCE || symbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED)
        require(symbol is KtFirSymbol<*>)

        val containingDeclaration = getContainingPsi(symbol)

        return with(analysisSession) {
            val containingSymbol = containingDeclaration.getSymbol()
            check(containingSymbol is KtSymbolWithKind)
            containingSymbol
        }
    }

    private fun getContainingPsi(symbol: KtFirSymbol<*>): KtDeclaration {
        val source = symbol.firSymbol.source
        val thisSource = when (source?.kind) {
            null -> error("PSI should present for declaration built by Kotlin code")
            KtFakeSourceElementKind.ImplicitConstructor ->
                return source.psi as KtDeclaration
            KtFakeSourceElementKind.PropertyFromParameter -> return source.psi?.parentOfType<KtPrimaryConstructor>()!!
            KtFakeSourceElementKind.DefaultAccessor -> return source.psi as KtProperty
            KtFakeSourceElementKind.ItLambdaParameter -> return source.psi as KtFunctionLiteral
            KtRealSourceElementKind -> source.psi!!
            else -> error("Unexpected FirSourceElement: kind=${source.kind} element=${source.psi!!::class.simpleName}")
        }

        return when (symbol.origin) {
            KtSymbolOrigin.SOURCE -> thisSource.getContainingKtDeclaration()
                ?: error("Containing declaration should present for non-toplevel declaration")
            KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> thisSource as KtDeclaration
            else -> error("Unsupported declaration origin ${symbol.origin}")
        }
    }

    private fun PsiElement.getContainingKtDeclaration(): KtDeclaration? =
        when (val container = this.parentOfType<KtDeclaration>()) {
            is KtDestructuringDeclaration -> container.parentOfType()
            else -> container
        }

    private fun getContainingDeclarationForLibrarySymbol(symbol: KtSymbolWithKind): KtSymbolWithKind = with(analysisSession) {
        require(symbol.origin == KtSymbolOrigin.LIBRARY || symbol.origin == KtSymbolOrigin.JAVA)
        check(symbol.symbolKind == KtSymbolKind.CLASS_MEMBER)

        val containingClassId = when (symbol) {
            is KtClassLikeSymbol -> {
                val classId = symbol.classIdIfNonLocal ?: error("classId should not be null for non-local declaration")
                classId.outerClassId
            }
            is KtFunctionSymbol -> {
                val fqName = symbol.callableIdIfNonLocal ?: error("callableIdIfNonLocal should not be null for non-local declaration")
                fqName.classId
            }
            is KtEnumEntrySymbol -> {
                val classId = symbol.containingEnumClassIdIfNonLocal ?: error("fqName should not be null for non-local declaration")
                classId.outerClassId
            }
            is KtPropertySymbol -> {
                val fqName = symbol.callableIdIfNonLocal ?: error("fqName should not be null for non-local declaration")
                fqName.classId
            }
            is KtConstructorSymbol -> {
                symbol.containingClassIdIfNonLocal
                    ?: error("fqName should not be null for non-local declaration")
            }
            else -> error("We should not have a ${symbol::class} from a library")
        } ?: error("outerClassId should not be null for member declaration")
        val containingClass = containingClassId.getCorrespondingToplevelClassOrObjectSymbol()
        return containingClass ?: error("Class with id $containingClassId should exists")
    }
}

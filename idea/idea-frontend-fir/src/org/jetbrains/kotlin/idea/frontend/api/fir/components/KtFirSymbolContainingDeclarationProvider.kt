/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.fir.low.level.api.util.parentOfType
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal class KtFirSymbolContainingDeclarationProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolContainingDeclarationProvider(), KtFirAnalysisSessionComponent {
    override fun getContainingDeclaration(symbol: KtSymbolWithKind): KtSymbolWithKind? {
        if (symbol is KtPackageSymbol) return null
        if (symbol.symbolKind == KtSymbolKind.TOP_LEVEL) return null
        if (symbol is KtCallableSymbol) {
            val classId = symbol.callableIdIfNonLocal?.classId
            if (classId != null) {
                with(analysisSession) {
                    return classId.getCorrespondingToplevelClassOrObjectSymbol()
                }
            }
        }
        return when (symbol.origin) {
            KtSymbolOrigin.SOURCE, KtSymbolOrigin.SOURCE_MEMBER_GENERATED ->
                getContainingDeclarationForKotlinInSourceSymbol(symbol)
            KtSymbolOrigin.LIBRARY, KtSymbolOrigin.JAVA, KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY ->
                getContainingDeclarationForLibrarySymbol(symbol)
            KtSymbolOrigin.PROPERTY_BACKING_FIELD -> getContainingDeclarationForBackingFieldSymbol(symbol)
            KtSymbolOrigin.INTERSECTION_OVERRIDE -> TODO()
            KtSymbolOrigin.SAM_CONSTRUCTOR -> TODO()
            KtSymbolOrigin.DELEGATED -> TODO()
        }
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
        val source = symbol.firRef.withFir(action = FirDeclaration::source)
        val thisSource = when (source?.kind) {
            null -> error("PSI should present for declaration built by Kotlin code")
            FirFakeSourceElementKind.ImplicitConstructor ->
                return source.psi as KtDeclaration
            FirFakeSourceElementKind.PropertyFromParameter -> return source.psi?.parentOfType<KtPrimaryConstructor>()!!
            FirRealSourceElementKind -> source.psi!!
            else -> error("Unexpected FirSourceElement: kind=${source.kind} element=${source.psi!!::class.simpleName}")
        }

        return when (symbol.origin) {
            KtSymbolOrigin.SOURCE -> thisSource.parentOfType()
                ?: error("Containing declaration should present for non-toplevel declaration")
            KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> thisSource as KtDeclaration
            else -> error("Unsupported declaration origin ${symbol.origin}")
        }
    }

    private fun getContainingDeclarationForLibrarySymbol(symbol: KtSymbolWithKind): KtSymbolWithKind = with(analysisSession) {
        require(symbol.origin == KtSymbolOrigin.LIBRARY || symbol.origin == KtSymbolOrigin.JAVA)
        check(symbol.symbolKind == KtSymbolKind.MEMBER)

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
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration

internal class KtFirSymbolContainingDeclarationProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolContainingDeclarationProvider(), KtFirAnalysisSessionComponent {
    override fun getContainingDeclaration(symbol: KtSymbolWithKind): KtSymbolWithKind? {
        if (symbol is KtPackageSymbol) return null
        if (symbol.symbolKind == KtSymbolKind.TOP_LEVEL) return null
        return when (symbol.origin) {
            KtSymbolOrigin.SOURCE, KtSymbolOrigin.SOURCE_MEMBER_GENERATED ->
                getContainingDeclarationForKotlinInSourceSymbol(symbol)
            KtSymbolOrigin.LIBRARY -> getContainingDeclarationForLibrarySymbol(symbol)
            KtSymbolOrigin.JAVA -> TODO()
            KtSymbolOrigin.SAM_CONSTRUCTOR -> TODO()
        }
    }

    private fun getContainingDeclarationForKotlinInSourceSymbol(symbol: KtSymbolWithKind): KtSymbolWithKind = with(analysisSession) {
        require(symbol.origin == KtSymbolOrigin.SOURCE || symbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED)
        val psi = symbol.psi ?: error("PSI should present for declaration built by Kotlin code")
        check(psi is KtDeclaration) { "PSI of kotlin declaration should be KtDeclaration" }
        val containingDeclaration = when (symbol.origin) {
            KtSymbolOrigin.SOURCE -> psi.parentOfType()
                ?: error("Containing declaration should present for non-toplevel declaration")
            KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> psi
            else -> error("Unsupported declaration origin ${symbol.origin}")
        }


        return with(analysisSession) {
            val containingSymbol = containingDeclaration.getSymbol()
            check(containingSymbol is KtSymbolWithKind)
            containingSymbol
        }
    }

    private fun getContainingDeclarationForLibrarySymbol(symbol: KtSymbolWithKind): KtSymbolWithKind = with(analysisSession) {
        require(symbol.origin == KtSymbolOrigin.LIBRARY)
        check(symbol.symbolKind == KtSymbolKind.MEMBER)

        val containingClassId = when (symbol) {
            is KtClassLikeSymbol -> {
                val classId = symbol.classIdIfNonLocal ?: error("classId should not be null for non-local declaration")
                classId.outerClassId
            }
            is KtFunctionSymbol -> {
                val fqName = symbol.callableIdIfNonLocal ?: error("fqName should not be null for non-local declaration")
                fqName.parent().let { ClassId.topLevel(it) }
            }
            is KtEnumEntrySymbol -> {
                val classId = symbol.containingEnumClassIdIfNonLocal ?: error("fqName should not be null for non-local declaration")
                classId.outerClassId
            }
            is KtPropertySymbol -> {
                val fqName = symbol.callableIdIfNonLocal ?: error("fqName should not be null for non-local declaration")
                fqName.parent().let { ClassId.topLevel(it) }
            }
            else -> error("We should not have a ${symbol::class} from a library")
        } ?: error("outerClassId should not be null for member declaration")
        val containingClass = containingClassId.getCorrespondingToplevelClassOrObjectSymbol()
        return containingClass ?: error("Class with id $containingClassId should exists")
    }
}
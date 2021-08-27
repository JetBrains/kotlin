/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.resolve.deprecation.Deprecation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.getDeprecationForCallSite
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolInfoProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirBackingFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken

internal class KtFirSymbolInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolInfoProvider(), KtFirAnalysisSessionComponent {
    override fun getDeprecation(symbol: KtSymbol): Deprecation? {
        if (symbol is KtFirBackingFieldSymbol || symbol is KtFirPackageSymbol) return null
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir {
            val firSymbol = it.symbol
            if (firSymbol is FirPropertySymbol) {
                firSymbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY)
            } else {
                firSymbol.getDeprecationForCallSite()
            }
        }
    }

    override fun getDeprecation(symbol: KtSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): Deprecation? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir { firDeclaration ->
            if (annotationUseSiteTarget != null) {
                firDeclaration.symbol.getDeprecationForCallSite(annotationUseSiteTarget)
            } else {
                firDeclaration.symbol.getDeprecationForCallSite()
            }
        }
    }

    override fun getGetterDeprecation(symbol: KtPropertySymbol): Deprecation? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir {
            it.symbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)
        }
    }

    override fun getSetterDeprecation(symbol: KtPropertySymbol): Deprecation? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir {
            it.symbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
        }
    }
}

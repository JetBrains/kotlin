/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSymbolInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getDeprecationForCallSite
import org.jetbrains.kotlin.fir.declarations.getJvmNameFromAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

internal class KtFirSymbolInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtSymbolInfoProvider(), KtFirAnalysisSessionComponent {
    override fun getDeprecation(symbol: KtSymbol): DeprecationInfo? {
        if (symbol is KtFirBackingFieldSymbol || symbol is KtFirPackageSymbol) return null
        require(symbol is KtFirSymbol<*>)
        return when (val firSymbol = symbol.firSymbol) {
            is FirPropertySymbol -> {
                firSymbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY)
            }
            else -> {
                firSymbol.getDeprecationForCallSite()
            }
        }

    }

    override fun getDeprecation(symbol: KtSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return if (annotationUseSiteTarget != null) {
            symbol.firSymbol.getDeprecationForCallSite(annotationUseSiteTarget)
        } else {
            symbol.firSymbol.getDeprecationForCallSite()
        }

    }

    override fun getGetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)

    }

    override fun getSetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
    }

    override fun getJavaGetterName(symbol: KtPropertySymbol): Name {
        require(symbol is KtFirSymbol<*>)
        if (symbol is KtFirSyntheticJavaPropertySymbol) {
            return symbol.javaGetterSymbol.name
        }
        val jvmName = run {
            val firProperty = symbol.firSymbol.fir as? FirProperty ?: return@run null
            firProperty.getJvmNameFromAnnotation(AnnotationUseSiteTarget.PROPERTY_GETTER) ?: firProperty.getter?.getJvmNameFromAnnotation()
        }
        return Name.identifier(jvmName ?: JvmAbi.getterName(symbol.name.identifier))
    }

    override fun getJavaSetterName(symbol: KtPropertySymbol): Name? {
        require(symbol is KtFirSymbol<*>)
        if (symbol is KtFirSyntheticJavaPropertySymbol) {
            return symbol.javaSetterSymbol?.name
        }
        return if (symbol.isVal) null
        else {
            val jvmName = run {
                val firProperty = symbol.firSymbol.fir as? FirProperty ?: return@run null
                firProperty.getJvmNameFromAnnotation(AnnotationUseSiteTarget.PROPERTY_GETTER)
                    ?: firProperty.setter?.getJvmNameFromAnnotation()
            }
            Name.identifier(jvmName ?: JvmAbi.setterName(symbol.name.identifier))
        }
    }
}

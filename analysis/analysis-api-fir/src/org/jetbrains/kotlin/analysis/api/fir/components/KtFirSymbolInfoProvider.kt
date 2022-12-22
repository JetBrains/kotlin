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
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getDeprecationForCallSite
import org.jetbrains.kotlin.fir.declarations.getJvmNameFromAnnotation
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

internal class KtFirSymbolInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtSymbolInfoProvider(), KtFirAnalysisSessionComponent {
    private val apiVersion = analysisSession.useSiteSession.languageVersionSettings.apiVersion

    override fun getDeprecation(symbol: KtSymbol): DeprecationInfo? {
        if (symbol is KtFirBackingFieldSymbol || symbol is KtFirPackageSymbol || symbol is KtReceiverParameterSymbol) return null
        require(symbol is KtFirSymbol<*>) { "${this::class}" }
        return when (val firSymbol = symbol.firSymbol) {
            is FirPropertySymbol -> {
                firSymbol.getDeprecationForCallSite(apiVersion, AnnotationUseSiteTarget.PROPERTY)
            }
            else -> {
                firSymbol.getDeprecationForCallSite(apiVersion)
            }
        }
    }

    override fun getDeprecation(symbol: KtSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return if (annotationUseSiteTarget != null) {
            symbol.firSymbol.getDeprecationForCallSite(apiVersion, annotationUseSiteTarget)
        } else {
            symbol.firSymbol.getDeprecationForCallSite(apiVersion)
        }

    }

    override fun getGetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(apiVersion, AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)

    }

    override fun getSetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(apiVersion, AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
    }

    override fun getJavaGetterName(symbol: KtPropertySymbol): Name {
        require(symbol is KtFirSymbol<*>)
        if (symbol is KtFirSyntheticJavaPropertySymbol) {
            return symbol.javaGetterSymbol.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        return getJvmName(firProperty, isSetter = false)
    }

    override fun getJavaSetterName(symbol: KtPropertySymbol): Name? {
        require(symbol is KtFirSymbol<*>)
        if (symbol is KtFirSyntheticJavaPropertySymbol) {
            return symbol.javaSetterSymbol?.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        if (firProperty.isVal) return null

        return getJvmName(firProperty, isSetter = true)
    }

    private fun getJvmName(property: FirProperty, isSetter: Boolean): Name {
        if (property.hasAnnotation(StandardClassIds.Annotations.JvmField, analysisSession.useSiteSession)) return property.name
        return Name.identifier(getJvmNameAsString(property, isSetter))
    }

    private fun getJvmNameAsString(property: FirProperty, isSetter: Boolean): String {
        val useSiteTarget = if (isSetter) AnnotationUseSiteTarget.PROPERTY_SETTER else AnnotationUseSiteTarget.PROPERTY_GETTER
        val jvmNameFromProperty = property.getJvmNameFromAnnotation(analysisSession.useSiteSession, useSiteTarget)
        if (jvmNameFromProperty != null) {
            return jvmNameFromProperty
        }

        val accessor = if (isSetter) property.setter else property.getter
        val jvmNameFromAccessor = accessor?.getJvmNameFromAnnotation(analysisSession.useSiteSession)
        if (jvmNameFromAccessor != null) {
            return jvmNameFromAccessor
        }

        val identifier = property.name.identifier
        return if (isSetter) JvmAbi.setterName(identifier) else JvmAbi.getterName(identifier)
    }
}

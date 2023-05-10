/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSymbolInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
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
        if (symbol is KtFirPackageSymbol || symbol is KtReceiverParameterSymbol) return null
        require(symbol is KtFirSymbol<*>) { "${this::class}" }

        // Optimization: Avoid building `firSymbol` of `KtFirPsiJavaClassSymbol` if it definitely isn't deprecated.
        if (symbol is KtFirPsiJavaClassSymbol && !symbol.mayHaveDeprecation()) {
            return null
        }

        return when (val firSymbol = symbol.firSymbol) {
            is FirPropertySymbol -> {
                firSymbol.getDeprecationForCallSite(apiVersion, AnnotationUseSiteTarget.PROPERTY)
            }
            is FirBackingFieldSymbol -> {
                firSymbol.getDeprecationForCallSite(apiVersion, AnnotationUseSiteTarget.FIELD)
            }
            else -> {
                firSymbol.getDeprecationForCallSite(apiVersion)
            }
        }
    }

    private fun KtFirPsiJavaClassSymbol.mayHaveDeprecation(): Boolean {
        if (!hasAnnotations) return false

        // Check the simple names of the Java annotations. While presence of such an annotation name does not prove deprecation, it is a
        // necessary condition for it. Type aliases are not a problem here: Java code cannot access Kotlin type aliases. (Currently,
        // deprecation annotation type aliases do not work in Kotlin, either, but this might change in the future.)
        return annotationSimpleNames.any { it != null && it in deprecationAnnotationSimpleNames }
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
        return symbol.firSymbol.getDeprecationForCallSite(
            apiVersion,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.PROPERTY,
        )

    }

    override fun getSetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(
            apiVersion,
            AnnotationUseSiteTarget.PROPERTY_SETTER,
            AnnotationUseSiteTarget.PROPERTY,
        )
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

    override fun getAnnotationApplicableTargets(symbol: KtClassOrObjectSymbol): Set<KotlinTarget>? {
        requireIsInstance<KtFirSymbol<*>>(symbol)
        if (symbol !is KtFirNamedClassOrObjectSymbol) return null
        if (symbol.firSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null
        return symbol.firSymbol.getAllowedAnnotationTargets(analysisSession.useSiteSession)
    }

    private fun getJvmName(property: FirProperty, isSetter: Boolean): Name {
        if (property.backingField?.symbol?.hasAnnotation(StandardClassIds.Annotations.JvmField, analysisSession.useSiteSession) == true) {
            return property.name
        }
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

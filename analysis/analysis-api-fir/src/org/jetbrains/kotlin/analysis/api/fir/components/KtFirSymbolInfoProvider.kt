/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
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
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo

internal class KaFirSymbolInfoProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken
) : KaSymbolInfoProvider(), KaFirSessionComponent {
    private val apiVersion = analysisSession.useSiteSession.languageVersionSettings.apiVersion

    override fun getDeprecation(symbol: KaSymbol): DeprecationInfo? {
        if (symbol is KaFirPackageSymbol || symbol is KaReceiverParameterSymbol) return null
        require(symbol is KaFirSymbol<*>) { "${this::class}" }

        // Optimization: Avoid building `firSymbol` of `KtFirPsiJavaClassSymbol` if it definitely isn't deprecated.
        if (symbol is KaFirPsiJavaClassSymbol && !symbol.mayHaveDeprecation()) {
            return null
        }

        return when (val firSymbol = symbol.firSymbol) {
            is FirPropertySymbol -> {
                firSymbol.getDeprecationForCallSite(analysisSession.useSiteSession, AnnotationUseSiteTarget.PROPERTY)
            }
            is FirBackingFieldSymbol -> {
                firSymbol.getDeprecationForCallSite(analysisSession.useSiteSession, AnnotationUseSiteTarget.FIELD)
            }
            else -> {
                firSymbol.getDeprecationForCallSite(analysisSession.useSiteSession)
            }
        }?.toDeprecationInfo()
    }

    private fun KaFirPsiJavaClassSymbol.mayHaveDeprecation(): Boolean {
        if (!hasAnnotations) return false

        // Check the simple names of the Java annotations. While presence of such an annotation name does not prove deprecation, it is a
        // necessary condition for it. Type aliases are not a problem here: Java code cannot access Kotlin type aliases. (Currently,
        // deprecation annotation type aliases do not work in Kotlin, either, but this might change in the future.)
        val deprecationAnnotationSimpleNames = analysisSession.useSiteSession.annotationPlatformSupport.deprecationAnnotationsSimpleNames
        return annotationSimpleNames.any { it != null && it in deprecationAnnotationSimpleNames }
    }

    override fun getDeprecation(symbol: KaSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? {
        require(symbol is KaFirSymbol<*>)
        return if (annotationUseSiteTarget != null) {
            symbol.firSymbol.getDeprecationForCallSite(analysisSession.useSiteSession, annotationUseSiteTarget)
        } else {
            symbol.firSymbol.getDeprecationForCallSite(analysisSession.useSiteSession)
        }?.toDeprecationInfo()
    }

    override fun getGetterDeprecation(symbol: KaPropertySymbol): DeprecationInfo? {
        require(symbol is KaFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(
            analysisSession.useSiteSession,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.PROPERTY,
        )?.toDeprecationInfo()

    }

    override fun getSetterDeprecation(symbol: KaPropertySymbol): DeprecationInfo? {
        require(symbol is KaFirSymbol<*>)
        return symbol.firSymbol.getDeprecationForCallSite(
            analysisSession.useSiteSession,
            AnnotationUseSiteTarget.PROPERTY_SETTER,
            AnnotationUseSiteTarget.PROPERTY,
        )?.toDeprecationInfo()
    }

    private fun FirDeprecationInfo.toDeprecationInfo(): DeprecationInfo {
        // We pass null as the message, otherwise we can trigger a contract violation
        // as getMessage will call lazyResolveToPhase(ANNOTATION_ARGUMENTS)
        // TODO(KT-67823) stop exposing compiler internals, as the message isn't actually required by the callers.
        return SimpleDeprecationInfo(deprecationLevel, propagatesToOverrides, null)
    }

    override fun getJavaGetterName(symbol: KaPropertySymbol): Name {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaFirSyntheticJavaPropertySymbol) {
            return symbol.javaGetterSymbol.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        return getJvmName(firProperty, isSetter = false)
    }

    override fun getJavaSetterName(symbol: KaPropertySymbol): Name? {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaFirSyntheticJavaPropertySymbol) {
            return symbol.javaSetterSymbol?.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        if (firProperty.isVal) return null

        return getJvmName(firProperty, isSetter = true)
    }

    override fun getAnnotationApplicableTargets(symbol: KaClassOrObjectSymbol): Set<KotlinTarget>? {
        requireIsInstance<KaFirSymbol<*>>(symbol)
        if (symbol !is KaFirNamedClassOrObjectSymbolBase) return null
        if (symbol.firSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null
        return symbol.firSymbol.getAllowedAnnotationTargets(analysisSession.useSiteSession)
    }

    private fun getJvmName(property: FirProperty, isSetter: Boolean): Name {
        if (property.backingField?.symbol?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField, analysisSession.useSiteSession) == true) {
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

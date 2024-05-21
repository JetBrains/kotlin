/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

public abstract class KaSymbolInfoProvider : KaSessionComponent() {
    public abstract fun getDeprecation(symbol: KaSymbol): DeprecationInfo?
    public abstract fun getDeprecation(symbol: KaSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo?
    public abstract fun getGetterDeprecation(symbol: KaPropertySymbol): DeprecationInfo?
    public abstract fun getSetterDeprecation(symbol: KaPropertySymbol): DeprecationInfo?

    public abstract fun getJavaGetterName(symbol: KaPropertySymbol): Name
    public abstract fun getJavaSetterName(symbol: KaPropertySymbol): Name?

    public abstract fun getAnnotationApplicableTargets(symbol: KaClassOrObjectSymbol): Set<KotlinTarget>?
}

public typealias KtSymbolInfoProvider = KaSymbolInfoProvider

public interface KaSymbolInfoProviderMixIn : KaSessionMixIn {
    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol is not deprecated.
     */
    public val KaSymbol.deprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            analysisSession.symbolInfoProvider.getDeprecation(this)
        }

    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol is not deprecated.
     */
    public fun KaSymbol.getDeprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? =
        withValidityAssertion { analysisSession.symbolInfoProvider.getDeprecation(this, annotationUseSiteTarget) }

    /**
     * Gets the deprecation status of the getter of this property symbol. Returns null if the getter is not deprecated.
     */
    public val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion { analysisSession.symbolInfoProvider.getGetterDeprecation(this) }

    /**
     * Gets the deprecation status of the setter of this property symbol. Returns null if the setter it not deprecated or the property does
     * not have a setter.
     */
    public val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion { analysisSession.symbolInfoProvider.getSetterDeprecation(this) }

    public val KaPropertySymbol.javaGetterName: Name
        get() = withValidityAssertion {
            analysisSession.symbolInfoProvider.getJavaGetterName(this)
        }

    public val KaPropertySymbol.javaSetterName: Name?
        get() = withValidityAssertion {
            analysisSession.symbolInfoProvider.getJavaSetterName(this)
        }

    /** Gets the set of applicable targets for an annotation class symbol. Returns `null` if the symbol is not an annotation class. */
    public val KaClassOrObjectSymbol.annotationApplicableTargets: Set<KotlinTarget>?
        get() = withValidityAssertion { analysisSession.symbolInfoProvider.getAnnotationApplicableTargets(this) }
}

public typealias KtSymbolInfoProviderMixIn = KaSymbolInfoProviderMixIn
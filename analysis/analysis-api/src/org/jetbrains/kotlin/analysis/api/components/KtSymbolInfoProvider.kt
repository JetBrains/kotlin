/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.Name

public abstract class KtSymbolInfoProvider : KtAnalysisSessionComponent() {
    public abstract fun getDeprecation(symbol: KtSymbol): DeprecationInfo?
    public abstract fun getDeprecation(symbol: KtSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo?
    public abstract fun getGetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo?
    public abstract fun getSetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo?

    public abstract fun getJavaGetterName(symbol: KtPropertySymbol): Name
    public abstract fun getJavaSetterName(symbol: KtPropertySymbol): Name?
}

public interface KtSymbolInfoProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol it not deprecated.
     */
    public val KtSymbol.deprecationStatus: DeprecationInfo? get() = analysisSession.symbolInfoProvider.getDeprecation(this)

    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol it not deprecated.
     */
    public fun KtSymbol.getDeprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? =
        analysisSession.symbolInfoProvider.getDeprecation(this)

    /**
     * Gets the deprecation status of the getter of this property symbol. Returns null if the getter it not deprecated.
     */
    public val KtPropertySymbol.getterDeprecationStatus: DeprecationInfo?
        get() = analysisSession.symbolInfoProvider.getGetterDeprecation(this)

    /**
     * Gets the deprecation status of the setter of this property symbol. Returns null if the setter it not deprecated or the property does
     * not have a setter.
     */
    public val KtPropertySymbol.setterDeprecationStatus: DeprecationInfo?
        get() = analysisSession.symbolInfoProvider.getSetterDeprecation(this)

    public val KtPropertySymbol.javaGetterName: Name get() = analysisSession.symbolInfoProvider.getJavaGetterName(this)
    public val KtPropertySymbol.javaSetterName: Name? get() = analysisSession.symbolInfoProvider.getJavaSetterName(this)
}

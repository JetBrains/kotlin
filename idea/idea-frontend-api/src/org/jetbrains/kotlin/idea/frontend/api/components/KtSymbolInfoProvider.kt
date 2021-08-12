/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.descriptors.Deprecation
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

public abstract class KtSymbolInfoProvider : KtAnalysisSessionComponent() {
    public abstract fun getDeprecation(symbol: KtSymbol): Deprecation?
    public abstract fun getGetterDeprecation(symbol: KtPropertySymbol): Deprecation?
    public abstract fun getSetterDeprecation(symbol: KtPropertySymbol): Deprecation?
}

public interface KtSymbolInfoProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol it not deprecated.
     */
    public val KtSymbol.deprecationStatus: Deprecation? get() = analysisSession.symbolInfoProvider.getDeprecation(this)

    /**
     * Gets the deprecation status of the getter of this property symbol. Returns null if the getter it not deprecated.
     */
    public val KtPropertySymbol.getterDeprecationStatus: Deprecation?
        get() = analysisSession.symbolInfoProvider.getGetterDeprecation(this)

    /**
     * Gets the deprecation status of the setter of this property symbol. Returns null if the setter it not deprecated or the property does
     * not have a setter.
     */
    public val KtPropertySymbol.setterDeprecationStatus: Deprecation?
        get() = analysisSession.symbolInfoProvider.getSetterDeprecation(this)
}
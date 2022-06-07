/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol

public abstract class KtInheritorsProvider : KtAnalysisSessionComponent() {
    public abstract fun getInheritorsOfSealedClass(classSymbol: KtNamedClassOrObjectSymbol): List<KtNamedClassOrObjectSymbol>
    public abstract fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol>
}

public interface KtInheritorsProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtNamedClassOrObjectSymbol.getSealedClassInheritors(): List<KtNamedClassOrObjectSymbol> =
        withValidityAssertion { analysisSession.inheritorsProvider.getInheritorsOfSealedClass(this) }

    public fun KtNamedClassOrObjectSymbol.getEnumEntries(): List<KtEnumEntrySymbol> =
        withValidityAssertion { analysisSession.inheritorsProvider.getEnumEntries(this) }
}
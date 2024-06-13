/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol

public abstract class KaInheritorsProvider : KaSessionComponent() {
    public abstract fun getInheritorsOfSealedClass(classSymbol: KaNamedClassOrObjectSymbol): List<KaNamedClassOrObjectSymbol>
    public abstract fun getEnumEntries(classSymbol: KaNamedClassOrObjectSymbol): List<KaEnumEntrySymbol>
}

public typealias KtInheritorsProvider = KaInheritorsProvider

public interface KaInheritorsProviderMixIn : KaSessionMixIn {
    public fun KaNamedClassOrObjectSymbol.getSealedClassInheritors(): List<KaNamedClassOrObjectSymbol> =
        withValidityAssertion { analysisSession.inheritorsProvider.getInheritorsOfSealedClass(this) }

    public fun KaNamedClassOrObjectSymbol.getEnumEntries(): List<KaEnumEntrySymbol> =
        withValidityAssertion { analysisSession.inheritorsProvider.getEnumEntries(this) }
}

public typealias KtInheritorsProviderMixIn = KaInheritorsProviderMixIn
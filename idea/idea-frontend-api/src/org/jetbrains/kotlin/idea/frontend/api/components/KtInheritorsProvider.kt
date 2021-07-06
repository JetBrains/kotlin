/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol

public abstract class KtInheritorsProvider : KtAnalysisSessionComponent() {
    public abstract fun getInheritorsOfSealedClass(classSymbol: KtNamedClassOrObjectSymbol): List<KtNamedClassOrObjectSymbol>
    public abstract fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol>
}

public interface KtInheritorsProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtNamedClassOrObjectSymbol.getSealedClassInheritors(): List<KtNamedClassOrObjectSymbol> =
        analysisSession.inheritorsProvider.getInheritorsOfSealedClass(this)

    public fun KtNamedClassOrObjectSymbol.getEnumEntries(): List<KtEnumEntrySymbol> =
        analysisSession.inheritorsProvider.getEnumEntries(this)
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol

abstract class KtInheritorsProvider : KtAnalysisSessionComponent() {
    abstract fun getInheritorsOfSealedClass(classSymbol: KtNamedClassOrObjectSymbol): List<KtNamedClassOrObjectSymbol>
    abstract fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol>
}

interface KtInheritorsProviderMixIn : KtAnalysisSessionMixIn {
     fun KtNamedClassOrObjectSymbol.getSealedClassInheritors(): List<KtNamedClassOrObjectSymbol> =
         analysisSession.inheritorsProvider.getInheritorsOfSealedClass(this)

     fun KtNamedClassOrObjectSymbol.getEnumEntries(): List<KtEnumEntrySymbol> =
         analysisSession.inheritorsProvider.getEnumEntries(this)
}
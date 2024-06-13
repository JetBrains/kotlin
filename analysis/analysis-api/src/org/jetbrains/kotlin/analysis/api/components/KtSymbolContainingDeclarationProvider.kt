/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public abstract class KaSymbolContainingDeclarationProvider : KaSessionComponent() {
    public abstract fun getContainingDeclaration(symbol: KaSymbol): KaDeclarationSymbol?

    public abstract fun getContainingFileSymbol(symbol: KaSymbol): KaFileSymbol?

    public abstract fun getContainingJvmClassName(symbol: KaCallableSymbol): String?

    public abstract fun getContainingModule(symbol: KaSymbol): KtModule
}

public typealias KtSymbolContainingDeclarationProvider = KaSymbolContainingDeclarationProvider

public interface KaSymbolContainingDeclarationProviderMixIn : KaSessionMixIn {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public fun KaSymbol.getContainingSymbol(): KaDeclarationSymbol? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingDeclaration(this) }

    /**
     * Returns containing [KtFile] as [KaFileSymbol]
     *
     * Caveat: returns `null` if the given symbol is already [KaFileSymbol], since there is no containing file.
     *  Similarly, no containing file for libraries and Java, hence `null`.
     */
    public fun KaSymbol.getContainingFileSymbol(): KaFileSymbol? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingFileSymbol(this) }

    /**
     * Returns containing JVM class name for [KaCallableSymbol]
     *
     *   even for deserialized callables! (which is useful to look up the containing facade in [PsiElement])
     *   for regular, non-local callables from source, it is a mere conversion of [ClassId] inside [CallableId]
     *
     * The returned JVM class name is of fully qualified name format, e.g., foo.bar.Baz.Companion
     *
     * Note that this API is applicable for common or JVM modules only, and returns `null` for non-JVM modules.
     */
    public fun KaCallableSymbol.getContainingJvmClassName(): String? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingJvmClassName(this) }

    public fun KaSymbol.getContainingModule(): KtModule =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingModule(this) }
}

public typealias KtSymbolContainingDeclarationProviderMixIn = KaSymbolContainingDeclarationProviderMixIn
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public abstract class KtSymbolContainingDeclarationProvider : KtAnalysisSessionComponent() {
    public abstract fun getContainingDeclaration(symbol: KtSymbol): KtDeclarationSymbol?

    public abstract fun getContainingFileSymbol(symbol: KtSymbol): KtFileSymbol?

    public abstract fun getContainingJvmClassName(symbol: KtCallableSymbol): String?

    public abstract fun getContainingModule(symbol: KtSymbol): KtModule
}

public interface KtSymbolContainingDeclarationProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public fun KtSymbol.getContainingSymbol(): KtDeclarationSymbol? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingDeclaration(this) }

    /**
     * Returns containing [KtFile] as [KtFileSymbol]
     *
     * Caveat: returns `null` if the given symbol is already [KtFileSymbol], since there is no containing file.
     *  Similarly, no containing file for libraries and Java, hence `null`.
     */
    public fun KtSymbol.getContainingFileSymbol(): KtFileSymbol? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingFileSymbol(this) }

    /**
     * Returns containing JVM class name for [KtCallableSymbol]
     *
     *   even for deserialized callables! (which is useful to look up the containing facade in [PsiElement])
     *   for regular, non-local callables from source, it is a mere conversion of [ClassId] inside [CallableId]
     *
     * The returned JVM class name is of fully qualified name format, e.g., foo.bar.Baz.Companion
     *
     * Note that this API is applicable for common or JVM modules only, and returns `null` for non-JVM modules.
     */
    public fun KtCallableSymbol.getContainingJvmClassName(): String? =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingJvmClassName(this) }

    public fun KtSymbol.getContainingModule(): KtModule =
        withValidityAssertion { analysisSession.containingDeclarationProvider.getContainingModule(this) }
}
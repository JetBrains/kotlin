/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

public abstract class KaSymbolContainingDeclarationProvider : KaSessionComponent() {
    public abstract fun getContainingJvmClassName(symbol: KaCallableSymbol): String?
}

public typealias KtSymbolContainingDeclarationProvider = KaSymbolContainingDeclarationProvider

public interface KaSymbolContainingDeclarationProviderMixIn : KaSessionMixIn {
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
}

public typealias KtSymbolContainingDeclarationProviderMixIn = KaSymbolContainingDeclarationProviderMixIn
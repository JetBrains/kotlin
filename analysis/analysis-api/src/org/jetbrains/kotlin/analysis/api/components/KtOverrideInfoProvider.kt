/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol

public abstract class KaOverrideInfoProvider : KaSessionComponent() {
    public abstract fun isVisible(memberSymbol: KaCallableSymbol, classSymbol: KaClassOrObjectSymbol): Boolean
}

public typealias KtOverrideInfoProvider = KaOverrideInfoProvider

public interface KaMemberSymbolProviderMixin : KaSessionMixIn {

    /** Checks if the given symbol (possibly a symbol inherited from a super class) is visible in the given class. */
    public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassOrObjectSymbol): Boolean =
        withValidityAssertion { analysisSession.overrideInfoProvider.isVisible(this, classSymbol) }
}

public typealias KtMemberSymbolProviderMixin = KaMemberSymbolProviderMixin
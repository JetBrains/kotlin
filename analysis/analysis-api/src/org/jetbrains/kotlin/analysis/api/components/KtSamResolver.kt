/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol

public abstract class KaSamResolver : KaSessionComponent() {
    public abstract fun getSamConstructor(symbol: KaClassLikeSymbol): KaSamConstructorSymbol?
}

public typealias KtSamResolver = KaSamResolver

public interface KaSamResolverMixIn : KaSessionMixIn {
    /**
     * Returns [KaSamConstructorSymbol] if the given [KaClassLikeSymbol] is a functional interface type, a.k.a. SAM.
     */
    public fun KaClassLikeSymbol.getSamConstructor(): KaSamConstructorSymbol? =
        withValidityAssertion { analysisSession.samResolver.getSamConstructor(this) }
}

public typealias KtSamResolverMixIn = KaSamResolverMixIn
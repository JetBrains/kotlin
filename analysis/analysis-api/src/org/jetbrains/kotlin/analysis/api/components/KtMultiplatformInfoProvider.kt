/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

public abstract class KaMultiplatformInfoProvider : KaSessionComponent() {
    public abstract fun getExpectForActual(actual: KaDeclarationSymbol): List<KaDeclarationSymbol>
}

public typealias KtMultiplatformInfoProvider = KaMultiplatformInfoProvider

public interface KaMultiplatformInfoProviderMixin : KaSessionMixIn {

    /**
     * Gives expect symbol for the actual one if it is available.
     *
     * @return a single expect declaration corresponds to the [KaDeclarationSymbol] on valid code or multiple expects in a case of erroneous code with multiple expects.
     **/
    public fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> =
        withValidityAssertion { analysisSession.multiplatformInfoProvider.getExpectForActual(this) }
}

public typealias KtMultiplatformInfoProviderMixin = KaMultiplatformInfoProviderMixin
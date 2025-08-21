/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol

@KaImplementationDetail
abstract class KaBaseSymbolInformationProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaSymbolInformationProvider {
    override val KaKotlinPropertySymbol.isInline: Boolean
        get() = withValidityAssertion {
            getter?.isInline == true && (isVal || setter?.isInline == true)
        }
}
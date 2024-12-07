/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSourceProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

internal class KaFe10SourceProvider(
    override val analysisSessionProvider: () -> KaSession
) : KaSessionComponent<KaSession>(), KaSourceProvider {
    override val KaDeclarationSymbol.klibSourceFileName: String?
        get() = withValidityAssertion {
            throw NotImplementedError("Method is not implemented for FE 1.0")
        }
}

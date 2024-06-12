/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KaSourceProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFirSourceProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaSourceProvider {
    override val KaDeclarationSymbol.klibSourceFileName: String?
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)
            firSymbol.klibSourceFile?.name
        }
}

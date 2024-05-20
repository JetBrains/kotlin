/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaKlibSourceFileNameProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFe10KlibSourceFileNameProvider(
    override val analysisSession: KaSession,
) : KaKlibSourceFileNameProvider() {
    override fun getKlibSourceFileName(declaration: KaDeclarationSymbol): String? {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}

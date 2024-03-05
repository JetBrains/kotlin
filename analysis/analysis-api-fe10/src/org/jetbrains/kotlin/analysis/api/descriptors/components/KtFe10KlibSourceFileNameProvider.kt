/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtKlibSourceFileNameProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFe10KlibSourceFileNameProvider(
    override val analysisSession: KtAnalysisSession,
) : KtKlibSourceFileNameProvider() {
    override fun getKlibSourceFileName(declaration: KtDeclarationSymbol): String? {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}

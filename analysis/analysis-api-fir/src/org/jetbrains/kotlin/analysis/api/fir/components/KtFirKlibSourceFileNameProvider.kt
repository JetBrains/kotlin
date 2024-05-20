/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KaKlibSourceFileNameProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFirKlibSourceFileNameProvider(
    override val analysisSession: KaFirSession,
) : KaKlibSourceFileNameProvider() {
    override fun getKlibSourceFileName(declaration: KaDeclarationSymbol): String? {
        require(declaration is KaFirSymbol<*>)
        val sourceFile = declaration.firSymbol.klibSourceFile ?: return null
        return sourceFile.name
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KtKlibSourceFileNameProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFirKlibSourceFileNameProvider(
    override val analysisSession: KtFirAnalysisSession,
) : KtKlibSourceFileNameProvider() {
    override fun getKlibSourceFileName(declaration: KtDeclarationSymbol): String? {
        require(declaration is KtFirSymbol<*>)
        val sourceFile = declaration.firSymbol.klibSourceFile ?: return null
        return sourceFile.name
    }
}

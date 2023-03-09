/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtOriginalPsiProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirOriginalPsiProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtOriginalPsiProvider(), KtFirAnalysisSessionComponent {
    override fun getOriginalDeclaration(declaration: KtDeclaration): KtDeclaration? = declaration.originalDeclaration

    override fun getOriginalKtFile(file: KtFile): KtFile? = file.originalKtFile

    override fun recordOriginalDeclaration(fakeDeclaration: KtDeclaration, originalDeclaration: KtDeclaration) {
        fakeDeclaration.originalDeclaration = originalDeclaration
    }

    override fun recordOriginalKtFile(fakeFile: KtFile, originalFile: KtFile) {
        fakeFile.originalKtFile = originalFile
    }
}
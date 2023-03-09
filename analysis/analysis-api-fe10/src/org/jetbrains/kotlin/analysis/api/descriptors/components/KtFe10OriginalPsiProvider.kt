/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtOriginalPsiProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

internal class KtFe10OriginalPsiProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtOriginalPsiProvider(), Fe10KtAnalysisSessionComponent {
    override fun getOriginalDeclaration(declaration: KtDeclaration): KtDeclaration? = null

    override fun getOriginalKtFile(file: KtFile): KtFile? = null

    override fun recordOriginalDeclaration(fakeDeclaration: KtDeclaration, originalDeclaration: KtDeclaration) {}

    override fun recordOriginalKtFile(fakeFile: KtFile, originalFile: KtFile) {}
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaOriginalPsiProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

@Suppress("OVERRIDE_DEPRECATION")
internal class KaFirOriginalPsiProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaOriginalPsiProvider, KaFirSessionComponent {
    override fun KtFile.recordOriginalKtFile(file: KtFile) = withValidityAssertion {
        originalKtFile = file
    }

    override fun KtDeclaration.recordOriginalDeclaration(declaration: KtDeclaration) = withValidityAssertion {
        originalDeclaration = declaration
    }

    override fun KtFile.getOriginalKtFile(): KtFile? = withValidityAssertion {
        return originalKtFile
    }

    override fun KtDeclaration.getOriginalDeclaration(): KtDeclaration? = withValidityAssertion {
        return originalDeclaration
    }
}
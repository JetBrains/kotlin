/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaOriginalPsiProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

@Suppress("OVERRIDE_DEPRECATION")
internal class KaFe10OriginalPsiProvider(
    override val analysisSessionProvider: () -> KaSession
) : KaSessionComponent<KaSession>(), KaOriginalPsiProvider {
    override fun KtFile.recordOriginalKtFile(file: KtFile) = withValidityAssertion {
        // Do nothing
    }

    override fun KtDeclaration.recordOriginalDeclaration(declaration: KtDeclaration) = withValidityAssertion {
        // Do nothing
    }

    override fun KtFile.getOriginalKtFile(): KtFile? = withValidityAssertion {
        return null
    }

    override fun KtDeclaration.getOriginalDeclaration(): KtDeclaration? = withValidityAssertion {
        return null
    }
}
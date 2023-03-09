/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtOriginalPsiProvider : KtAnalysisSessionComponent() {
    public abstract fun getOriginalDeclaration(declaration: KtDeclaration): KtDeclaration?
    public abstract fun getOriginalKtFile(file: KtFile): KtFile?

    public abstract fun recordOriginalDeclaration(fakeDeclaration: KtDeclaration, originalDeclaration: KtDeclaration)
    public abstract fun recordOriginalKtFile(fakeFile: KtFile, originalFile: KtFile)
}

public interface KtOriginalPsiProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * If [KtDeclaration] is a non-local declaration in a fake file analyzed in dependent session, returns the original declaration
     * for [this]. Otherwise, returns `null`.
     */
    public fun KtDeclaration.getOriginalDeclaration(): KtDeclaration? =
        withValidityAssertion { analysisSession.originalPsiProvider.getOriginalDeclaration(this) }

    /**
     * If [this] is a fake file analyzed in dependent session, returns the original file for [this]. Otherwise, returns `null`.
     */
    public fun KtFile.getOriginalKtFile(): KtFile? =
        withValidityAssertion { analysisSession.originalPsiProvider.getOriginalKtFile(this) }

    /**
     * Records [declaration] as an original declaration for [this].
     */
    public fun KtDeclaration.recordOriginalDeclaration(declaration: KtDeclaration) {
        withValidityAssertion { analysisSession.originalPsiProvider.recordOriginalDeclaration(this, declaration) }
    }

    /**
     * Records [file] as an original file for [this].
     */
    public fun KtFile.recordOriginalKtFile(file: KtFile) {
        withValidityAssertion { analysisSession.originalPsiProvider.recordOriginalKtFile(this, file) }
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

@Deprecated("Obsolete API")
public interface KaOriginalPsiProvider {
    /**
     * If [KtDeclaration] is a non-local declaration in a fake file analyzed in dependent session, returns the original declaration.
     * Otherwise, returns `null`.
     */
    @Deprecated("Obsolete API")
    public fun KtDeclaration.getOriginalDeclaration(): KtDeclaration?

    /**
     * If [this] is a fake file analyzed in dependent session, returns the original file for [this]. Otherwise, returns `null`.
     */
    @Deprecated("Obsolete API")
    public fun KtFile.getOriginalKtFile(): KtFile?

    /**
     * Records [declaration] as an original declaration for [this].
     */
    @Deprecated("Obsolete API")
    public fun KtDeclaration.recordOriginalDeclaration(declaration: KtDeclaration)

    /**
     * Records [file] as an original file for [this].
     */
    @Deprecated("Obsolete API")
    public fun KtFile.recordOriginalKtFile(file: KtFile)
}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement

/**
 * The mapper for PSI-based sources. It stores nothing: the path is taken from the PSI file of an element.
 */
class PsiSourcesToPathsMapper : SourcesToPathsMapper() {
    override fun getSourceFilePath(sourceElement: KtSourceElement): String? {
        return (sourceElement as KtPsiSourceElement).psi.containingFile?.virtualFile?.path
    }
}

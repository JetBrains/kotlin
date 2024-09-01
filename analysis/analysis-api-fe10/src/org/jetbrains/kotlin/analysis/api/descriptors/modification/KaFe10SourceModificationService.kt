/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.modification

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService

internal class KaFe10SourceModificationService : KaSourceModificationService {
    override fun handleElementModification(
        element: PsiElement,
        modificationType: KaElementModificationType,
    ) {
        throw UnsupportedOperationException("`${KaSourceModificationService::class.simpleName}` is not implemented in FE10.")
    }

    override fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement? {
        throw UnsupportedOperationException("`${KaSourceModificationService::class.simpleName}` is not implemented in FE10.")
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

private fun KtDiagnosticWithSource.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        requireWithAttachment(
            factory.psiType.isInstance(psiElement),
            { "${psiElement::class} is not a subtype of ${factory.psiType} for factory $factory" }
        ) {
            withPsiEntry("psi", psiElement)
            withPsiEntry("file", psiElement.containingFile)
        }
    }
}

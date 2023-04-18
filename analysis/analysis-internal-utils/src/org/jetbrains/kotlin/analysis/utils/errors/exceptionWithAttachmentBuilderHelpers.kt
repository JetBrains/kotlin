/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.psi.KtElement

public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, moduleFactory: (PsiElement) -> KtModule) {
    return withPsiEntry(name, psi, psi?.let(moduleFactory))
}

public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, module: KtModule?) {
    withEntry(name, psi) { psiElement ->
        when {
            !psiElement.isValid -> "INVALID PSI ${PsiInvalidElementAccessException.findOutInvalidationReason(psiElement)}"
            psiElement is KtElement -> psiElement.getElementTextInContext()
            else -> psiElement.text
        }
    }
    if (psi != null) {
        withKtModuleEntry("${name}KtModule", module)
    }
}

public fun ExceptionAttachmentBuilder.withKtModuleEntry(name: String, module: KtModule?) {
    withEntry(name, module) { ktModule -> ktModule.moduleDescription }
}

public fun ExceptionAttachmentBuilder.withClassEntry(name: String, element: Any?) {
    withEntry(name, element) { it::class.java.name }
}
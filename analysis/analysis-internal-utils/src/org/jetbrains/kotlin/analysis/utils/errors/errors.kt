/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.errorWithAttachment
import org.jetbrains.kotlin.utils.withAttachmentDetailed

public fun unexpectedElementError(elementName: String, element: Any?): Nothing {
    errorWithAttachment("Unexpected $elementName ${element?.let { it::class.simpleName }}") {
        withAttachment("element", element)
    }
}

public inline fun <reified ELEMENT> unexpectedElementError(element: Any?): Nothing {
    unexpectedElementError(ELEMENT::class.simpleName ?: ELEMENT::class.java.name, element)
}

public fun KotlinExceptionWithAttachments.withPsiAttachment(name: String, psi: PsiElement?): KotlinExceptionWithAttachments {
    withAttachmentDetailed(name, psi) { psiElement ->
        when (psiElement) {
            is KtElement -> psiElement.getElementTextInContext()
            else -> psiElement.text
        }
    }
    return this
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Retrieves the text of the [PsiElement] within its context.
 *
 * If the element is invalid, it returns a message indicating its invalidity.
 * In case of any exception during the process, a message with the stack trace is returned.
 *
 * @param psiElement The PSI element whose text within the broader context is to be retrieved.
 * @return A string representing the text of the PSI element within its context, or an error message in case of failure.
 */
fun getElementTextWithContext(psiElement: PsiElement): String = runCatching {
    if (!psiElement.isValid) {
        return "<invalid element $psiElement, " +
                "invalidation reason: ${PsiInvalidElementAccessException.findOutInvalidationReason(psiElement)}>"
    }

    @Suppress("LocalVariableName") val ELEMENT_TAG = "ELEMENT"
    val containingFile = psiElement.containingFile
    val context = psiElement.parentOfType("KtImportDirective")
        ?: psiElement.parentOfType("KtPackageDirective")
        ?: psiElement.parentOfType("KtDeclarationWithBody", "KtClassOrObject", "KtScript")
        ?: psiElement.parentOfType("KtProperty")
        ?: containingFile
    val elementTextInContext = buildString {
        context.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element === psiElement) append("<$ELEMENT_TAG>")
                if (element is LeafPsiElement) {
                    append(element.text)
                } else {
                    element.acceptChildren(this)
                }
                if (element === psiElement) append("</$ELEMENT_TAG>")
            }
        })
    }.trimIndent().trim()

    return buildString {
        appendLine("<File name: ${containingFile.name}, Physical: ${containingFile.isPhysical}>")
        append(elementTextInContext)
    }
}.getOrElse { throwable ->
    "EXCEPTION: Could not get element text in context due to an exception:\n$throwable\n${throwable.stackTraceToString()}>"
}

private fun PsiElement.parentOfType(vararg psiClassNames: String): PsiElement? {
    fun acceptsClass(javaClass: Class<*>): Boolean {
        if (javaClass.simpleName in psiClassNames) return true
        javaClass.superclass?.let { if (acceptsClass(it)) return true }
        for (superInterface in javaClass.interfaces) {
            if (acceptsClass(superInterface)) return true
        }
        return false
    }
    return generateSequence(this) { it.parent }
        .filter { it !is PsiFile }
        .firstOrNull { acceptsClass(it::class.java) }
}
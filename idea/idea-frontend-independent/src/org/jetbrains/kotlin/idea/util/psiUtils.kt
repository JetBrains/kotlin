/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

fun KtElement.getElementTextInContext(): String {
    val context = parentOfType<KtImportDirective>()
        ?: parentOfType<KtPackageDirective>()
        ?: containingDeclarationForPseudocode
        ?: containingKtFile
    val builder = StringBuilder()
    context.accept(object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element === this@getElementTextInContext) builder.append("<$ELEMENT_TAG>")
            if (element is LeafPsiElement) {
                builder.append(element.text)
            } else {
                element.acceptChildren(this)
            }
            if (element === this@getElementTextInContext) builder.append("</$ELEMENT_TAG>")
        }
    })
    return builder.toString().trimIndent().trim()
}
private const val ELEMENT_TAG = "ELEMENT"


/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.printer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

public inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

public fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>, withSelf: Boolean = true): Sequence<T> {
    return (if (withSelf) parentsWithSelf else parents).filterIsInstance(clazz)
}

public inline fun <reified T : PsiElement> PsiElement.parentsOfType(withSelf: Boolean = true): Sequence<T> =
    parentsOfType(T::class.java, withSelf)


public fun KtElement.getElementTextInContext(): String {
    @Suppress("LocalVariableName") val ELEMENT_TAG = "ELEMENT"
    val context = parentOfType<KtImportDirective>()
        ?: parentOfType<KtPackageDirective>()
        ?: PsiTreeUtil.getParentOfType(this, KtDeclarationWithBody::class.java, KtClassOrObject::class.java, KtScript::class.java)
        ?: getNonStrictParentOfType<KtProperty>()
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


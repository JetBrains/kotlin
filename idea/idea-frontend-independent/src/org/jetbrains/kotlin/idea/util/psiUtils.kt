/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.cfg.containingDeclarationForPseudocode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

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

fun PsiClass.classIdIfNonLocal(): ClassId? {
    if (this is KtLightClass) {
        return this.kotlinOrigin?.getClassId()
    }
    val packageName = (containingFile as? PsiJavaFile)?.packageName ?: return null
    val packageFqName = FqName(packageName)

    val classesNames = parentsOfType<KtDeclaration>().map { it.name }.toList().asReversed()
    if (classesNames.any { it == null }) return null
    return ClassId(packageFqName, FqName(classesNames.joinToString(separator = ".")), false)
}

fun KtExpression.resultingWhens(): List<KtWhenExpression> = when (this) {
    is KtWhenExpression -> listOf(this) + entries.map { it.expression?.resultingWhens() ?: listOf() }.flatten()
    is KtIfExpression -> (then?.resultingWhens() ?: listOf()) + (`else`?.resultingWhens() ?: listOf())
    is KtBinaryExpression -> (left?.resultingWhens() ?: listOf()) + (right?.resultingWhens() ?: listOf())
    is KtUnaryExpression -> this.baseExpression?.resultingWhens() ?: listOf()
    is KtBlockExpression -> statements.lastOrNull()?.resultingWhens() ?: listOf()
    else -> listOf()
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.cfg.containingDeclarationForPseudocode
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render

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

fun generateWhenBranches(element: KtWhenExpression, missingCases: List<WhenMissingCase>) {
    val psiFactory = KtPsiFactory(element)
    val whenCloseBrace = element.closeBrace ?: run {
        val craftingMaterials = psiFactory.createExpression("when(1){}") as KtWhenExpression
        if (element.rightParenthesis == null) {
            element.addAfter(
                craftingMaterials.rightParenthesis!!,
                element.subjectExpression ?: throw AssertionError("caller should have checked the presence of subject expression.")
            )
        }
        if (element.openBrace == null) {
            element.addAfter(craftingMaterials.openBrace!!, element.rightParenthesis!!)
        }
        element.addAfter(craftingMaterials.closeBrace!!, element.entries.lastOrNull() ?: element.openBrace!!)
        element.closeBrace!!
    }
    val elseBranch = element.entries.find { it.isElse }
    (whenCloseBrace.prevSibling as? PsiWhiteSpace)?.replace(psiFactory.createNewLine())
    for (case in missingCases) {
        val branchConditionText = when (case) {
            WhenMissingCase.Unknown,
            WhenMissingCase.NullIsMissing,
            is WhenMissingCase.BooleanIsMissing,
            is WhenMissingCase.ConditionTypeIsExpect -> case.branchConditionText
            is WhenMissingCase.IsTypeCheckIsMissing ->
                if (case.isSingleton) {
                    ""
                } else {
                    "is "
                } + case.classId.asSingleFqName().render()
            is WhenMissingCase.EnumCheckIsMissing -> case.callableId.asSingleFqName().render()
        }
        val entry = psiFactory.createWhenEntry("$branchConditionText -> TODO()")
        if (elseBranch != null) {
            element.addBefore(entry, elseBranch)
        } else {
            element.addBefore(entry, whenCloseBrace)
        }
    }
}

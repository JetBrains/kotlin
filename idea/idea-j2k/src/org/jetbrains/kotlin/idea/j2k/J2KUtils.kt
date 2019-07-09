/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.OldJavaToKotlinConverter
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory

fun PsiElement.j2kText(): String? =
    convertToKotlin()?.results?.single()?.text //TODO: insert imports

fun PsiElement.convertToKotlin(): org.jetbrains.kotlin.j2k.Result? {
    if (language != JavaLanguage.INSTANCE) return null

    val j2kConverter = OldJavaToKotlinConverter(project, ConverterSettings.defaultSettings, IdeaJavaToKotlinServices)
    return j2kConverter.elementsToKotlin(listOf(this))
}

fun PsiExpression.j2k(): KtExpression? {
    val text = j2kText() ?: return null
    return KtPsiFactory(project).createExpression(text)
}

fun PsiMember.j2k(): KtNamedDeclaration? {
    val text = j2kText() ?: return null
    return KtPsiFactory(project).createDeclaration(text)
}
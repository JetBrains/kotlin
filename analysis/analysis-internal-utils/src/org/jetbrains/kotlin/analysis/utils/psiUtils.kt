/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments

public val PsiClass.classId: ClassId?
    get() {
        val packageName = (containingFile as? PsiClassOwner)?.packageName ?: return null
        if (qualifiedName == null) return null

        val classesChain = generateSequence(this) { it.containingClass }
        if (classesChain.any { it is PsiAnonymousClass }) return null

        val classNames = classesChain.mapTo(mutableListOf()) { it.name }.asReversed()
        if (classNames.any { it == null }) return null

        return ClassId(FqName(packageName), FqName(classNames.joinToString(separator = ".")), isLocal = false)
    }



public fun PsiClass.isLocalClass(): Boolean {
    val qualifiedName = this.qualifiedName ?: return true
    val classId = classId ?: return true

    /*
    For a local class:
    qualifiedName: javax.swing.JSlider$1SmartHashtable.LabelUIResource
    classId.asFqNameString(): javax.swing.JSlider.SmartHashtable.LabelUIResource

    For a nested class with:
    qualifiedName: pckg.A$B
    classId.asFqNameString(): pckg.A.B

    For a class with $ in name:
    qualifiedName: pckg.With$InName
    classId.asFqNameString(): pckg.With$InName
     */
    return classId.asFqNameString().replace('$', '.') != qualifiedName.replace('$', '.')
}

/**
 * A common pattern of illegal code, where a declaration is preceded by an unclosed annotation, for example:
 *
 * ```kotlin
 * @Ann(
 * fun foo() = 42
 * ```
 * @see org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
 */
public fun KtDeclaration.isInsideDanglingModifierList(): Boolean {
    val argument = parent as? KtValueArgument ?: return false
    val modifierList = argument.parentOfType<KtDeclarationModifierList>() ?: return false
    if (modifierList.getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement) {
        return true // top level
    }
    if (argument.getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement) {
        return true // class member
    }
    return false
}
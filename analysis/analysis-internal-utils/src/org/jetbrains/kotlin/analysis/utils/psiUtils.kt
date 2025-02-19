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
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
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
 * Detects a common pattern of invalid code where a modifier list (e.g., annotation)
 * is dangling—unattached to a valid declaration—or left unclosed and followed by another declaration.
 *
 * ### Examples
 *
 * ```kotlin
 * class C1 {
 *     @Ann1 @Ann2
 * }
 *
 * class C2 {
 *     @Ann(
 *     fun foo() {}
 * }
 *
 * @Ann("argument"
 * fun foo() {}
 * ```
 * @see org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
 */
public fun KtModifierList.isNonLocalDanglingModifierList(): Boolean {
    val parent = parent
    val isLocal = when (parent) {
        is KtFile -> false
        is KtClassBody if (parent.parent as? KtClassOrObject)?.isLocal() == false -> false
        else -> true
    }

    if (isLocal) {
        // We ignore local modifier lists for LL file structure purposes
        return false
    }

    // A dangling modifier list is, by definition, syntactically invalid.
    // Given the variety of invalid code patterns, we rely on a simple best-effort check:
    // a modifier list is considered dangling if it's followed by a syntax error
    // or contains any syntax errors within its descendants recursively.
    return getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement || anyDescendantOfType<PsiErrorElement>()
}

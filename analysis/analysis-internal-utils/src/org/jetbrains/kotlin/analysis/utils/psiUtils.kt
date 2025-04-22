/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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
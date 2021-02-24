/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal abstract class FirLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    protected val owner: T
) : KtLightElementBase(owner), PsiModifierList, KtLightElement<KtModifierList, PsiModifierListOwner> {

    override val clsDelegate: PsiModifierListOwner
        get() = invalidAccess()

    override val kotlinOrigin: KtModifierList?
        get() = owner.kotlinOrigin?.modifierList

    override fun getParent() = owner

    override fun hasExplicitModifier(name: String) = hasModifierProperty(name)

    override fun setModifierProperty(name: String, value: Boolean) = cannotModify()
    override fun checkSetModifierProperty(name: String, value: Boolean) = throw IncorrectOperationException()
    override fun addAnnotation(qualifiedName: String): PsiAnnotation = cannotModify()

    override fun getApplicableAnnotations(): Array<out PsiAnnotation> = annotations

    override fun getAnnotations(): Array<out PsiAnnotation> = emptyArray() //TODO()
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? = null //TODO()

    override fun isEquivalentTo(another: PsiElement?) =
        another is FirLightModifierList<*> && owner == another.owner

    override fun isWritable() = false

    override fun toString() = "Light modifier list of $owner"

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}

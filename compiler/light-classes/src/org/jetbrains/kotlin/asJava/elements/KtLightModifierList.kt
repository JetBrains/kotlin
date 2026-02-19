/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner

abstract class KtLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    protected val owner: T
) : KtLightElementBase(owner), PsiModifierList, KtLightElement<KtModifierList, PsiModifierList> {
    private val _annotations by lazyPub {
        val annotations = computeAnnotations()
        annotationsFilter?.let(annotations::filter) ?: annotations
    }

    protected open val annotationsFilter: ((KtLightAbstractAnnotation) -> Boolean)? = null

    override val kotlinOrigin: KtModifierList?
        get() = owner.kotlinOrigin?.modifierList

    override fun getParent() = owner

    override fun hasExplicitModifier(name: String) = hasModifierProperty(name)

    private fun throwInvalidOperation(): Nothing = throw IncorrectOperationException()

    override fun setModifierProperty(name: String, value: Boolean): Unit = throwInvalidOperation()

    override fun checkSetModifierProperty(name: String, value: Boolean): Unit = throwInvalidOperation()

    override fun addAnnotation(qualifiedName: String): PsiAnnotation = throwInvalidOperation()

    override fun getApplicableAnnotations(): Array<out PsiAnnotation> = annotations

    override fun getAnnotations(): Array<out PsiAnnotation> = _annotations.toTypedArray()
    override fun findAnnotation(qualifiedName: String) = _annotations.firstOrNull { it.fqNameMatches(qualifiedName) }

    override fun isEquivalentTo(another: PsiElement?) =
        another is KtLightModifierList<*> && owner == another.owner

    override fun isWritable() = false

    override fun toString() = "Light modifier list of $owner"

    open fun nonSourceAnnotationsForAnnotationType(sourceAnnotations: List<PsiAnnotation>): List<KtLightAbstractAnnotation> = emptyList()

    abstract fun computeAnnotations(): List<KtLightAbstractAnnotation>
}
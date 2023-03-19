/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierList
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration

abstract class KtLightMemberImpl<out D : PsiMember>(
    override val lightMemberOrigin: LightMemberOriginForDeclaration?,
    private val containingClass: KtLightClass,
) : KtLightElementBase(containingClass), PsiMember, KtLightMember<D> {
    abstract override fun hasModifierProperty(name: String): Boolean

    abstract override fun getModifierList(): PsiModifierList?

    override fun toString(): String = "${this::class.java.simpleName}:$name"

    override fun getContainingClass() = containingClass

    abstract override fun getName(): String

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration)

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    abstract override fun getDocComment(): PsiDocComment?

    abstract override fun isDeprecated(): Boolean

    override fun isValid(): Boolean {
        return parent.isValid && lightMemberOrigin?.isValid() != false
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return this == another ||
                lightMemberOrigin?.isEquivalentTo(another) == true ||
                another is KtLightMember<*> && lightMemberOrigin?.isEquivalentTo(another.lightMemberOrigin) == true
    }
}

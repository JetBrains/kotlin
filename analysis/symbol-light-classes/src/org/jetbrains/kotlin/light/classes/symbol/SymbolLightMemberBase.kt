/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.psi.KtDeclaration

internal abstract class SymbolLightMemberBase<T : PsiMember>(
    override val lightMemberOrigin: LightMemberOrigin?,
    private val containingClass: SymbolLightClassBase,
) : KtLightElementBase(containingClass), PsiMember, KtLightMember<T> {
    val ktModule: KtModule get() = containingClass.ktModule

    override fun hasModifierProperty(name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

    override fun toString(): String = "${this::class.java.simpleName}:$name"

    override fun getContainingClass(): SymbolLightClassBase = containingClass

    abstract override fun getNameIdentifier(): PsiIdentifier?

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getDocComment(): PsiDocComment? = null //TODO()

    abstract override fun isDeprecated(): Boolean

    abstract override fun getName(): String

    override fun isValid(): Boolean = parent.isValid && lightMemberOrigin?.isValid() != false

    override fun isEquivalentTo(another: PsiElement?): Boolean = basicIsEquivalentTo(this, another as? PsiMethod)

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean
}
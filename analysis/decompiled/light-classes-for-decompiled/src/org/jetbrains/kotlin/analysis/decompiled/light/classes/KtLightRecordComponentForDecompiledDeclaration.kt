/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtParameter

internal class KtLightRecordComponentForDecompiledDeclaration(
    private val clsDelegate: PsiRecordComponent,
    recordHeader: KtLightRecordHeaderForDecompiledDeclaration,
    internal val containingClass: KtLightClassForDecompiledDeclarationBase,
    override val kotlinOrigin: KtParameter?,
) : KtLightElementBase(parent = recordHeader), PsiRecordComponent, KtLightElement<KtParameter, PsiRecordComponent>, PsiVariableEx {
    override fun hasModifierProperty(name: String): Boolean = clsDelegate.hasModifierProperty(name)

    override fun getModifierList(): PsiModifierList? = clsDelegate.modifierList

    override fun getContainingClass(): PsiClass = containingClass

    override fun getTypeElement(): PsiTypeElement? = clsDelegate.typeElement

    override fun getType(): PsiType = clsDelegate.type

    override fun getInitializer(): PsiExpression? = null

    override fun hasInitializer(): Boolean = false

    override fun computeConstantValue(): Any? = null

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? = null

    override fun getNameIdentifier(): PsiIdentifier? = clsDelegate.nameIdentifier

    override fun setName(name: String): PsiElement = cannotModify()

    override fun getName(): String = clsDelegate.name

    override fun normalizeDeclaration() {}

    override fun isVarArgs(): Boolean = false
    override fun copy(): PsiElement = this
    override fun clone(): Any = this
    override fun getOriginalElement(): PsiElement = clsDelegate

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return this == another ||
                another is KtLightRecordComponentForDecompiledDeclaration && clsDelegate.isEquivalentTo(another.clsDelegate) ||
                clsDelegate.isEquivalentTo(another)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitRecordComponent(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is KtLightRecordComponentForDecompiledDeclaration) return false
        if (name != other.name) return false
        if (containingClass != other.containingClass) return false
        return clsDelegate == other.clsDelegate
    }

    override fun hashCode(): Int = 31 * (31 * name.hashCode() + containingClass.hashCode()) + clsDelegate.hashCode()
}

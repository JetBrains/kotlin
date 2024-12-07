/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.LightMemberOriginForCompiledField
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightFieldForSourceDeclarationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.psi.KtDeclaration

open class KtLightFieldForDecompiledDeclaration(
    private val fldDelegate: PsiField,
    private val fldParent: KtLightClass,
    override val lightMemberOrigin: LightMemberOriginForCompiledField
) : KtLightElementBase(fldParent), PsiField, KtLightFieldForSourceDeclarationSupport, KtLightMember<PsiField>, PsiVariableEx {

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin.originalElement

    override fun hasModifierProperty(name: String): Boolean = fldDelegate.hasModifierProperty(name)

    override fun setInitializer(initializer: PsiExpression?) {
        fldDelegate.initializer = initializer
    }

    override fun getContainingClass(): KtLightClass = fldParent

    override fun normalizeDeclaration() = fldDelegate.normalizeDeclaration()

    override fun getNameIdentifier(): PsiIdentifier = fldDelegate.nameIdentifier

    override fun getName(): String = fldDelegate.name

    override fun getInitializer(): PsiExpression? = fldDelegate.initializer

    override fun getDocComment(): PsiDocComment? = fldDelegate.docComment

    override fun getTypeElement(): PsiTypeElement? = fldDelegate.typeElement

    override fun getModifierList(): PsiModifierList? = fldDelegate.modifierList

    override fun hasInitializer(): Boolean = fldDelegate.hasInitializer()

    override fun getType(): PsiType = fldDelegate.type

    override fun isDeprecated(): Boolean = fldDelegate.isDeprecated

    override fun setName(name: String): PsiElement = fldDelegate.setName(name)

    override fun computeConstantValue(): Any? = fldDelegate.computeConstantValue()

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? =
        (fldDelegate as? PsiVariableEx)?.computeConstantValue(visitedVars)

    override fun equals(other: Any?): Boolean = other === this ||
            other is KtLightFieldForDecompiledDeclaration &&
            name == other.name &&
            fldParent == other.fldParent &&
            fldDelegate == other.fldDelegate

    override fun hashCode(): Int = name.hashCode()

    override fun copy(): PsiElement = this

    override fun clone(): Any = this

    override fun toString(): String = "${this.javaClass.simpleName} of $fldParent"

    override fun isValid(): Boolean = parent.isValid

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return this == another ||
                another is KtLightFieldForDecompiledDeclaration && fldDelegate.isEquivalentTo(another.fldDelegate) ||
                fldDelegate.isEquivalentTo(another)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitField(this)
        } else {
            visitor.visitElement(this)
        }
    }
}

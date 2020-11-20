/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.navigation.NavigationItem
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.psi.KtParameter

internal abstract class FirLightParameter(containingDeclaration: FirLightMethod) : PsiVariable, NavigationItem,
    KtLightElement<KtParameter, PsiParameter>, KtLightParameter, KtLightElementBase(containingDeclaration) {

    override val clsDelegate: PsiParameter
        get() = invalidAccess()

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = invalidAccess()

    override fun getTypeElement(): PsiTypeElement? = null
    override fun getInitializer(): PsiExpression? = null
    override fun hasInitializer(): Boolean = false
    override fun computeConstantValue(): Any? = null
    override fun getNameIdentifier(): PsiIdentifier? = null

    abstract override fun getName(): String

    @Throws(IncorrectOperationException::class)
    override fun normalizeDeclaration() {
    }

    override fun setName(p0: String): PsiElement = TODO() //cannotModify()

    override val method: KtLightMethod = containingDeclaration

    override fun getDeclarationScope(): KtLightMethod = method

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitParameter(this)
        }
    }

    override fun toString(): String = "Fir Light Parameter $name"

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another as? PsiParameter)

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: method.navigationElement

    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: LocalSearchScope(this)

    override fun isValid() = parent.isValid

    abstract override fun getType(): PsiType

    override fun getContainingFile(): PsiFile = method.containingFile

    override fun getParent(): PsiElement = method.parameterList

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun isVarArgs(): Boolean
}
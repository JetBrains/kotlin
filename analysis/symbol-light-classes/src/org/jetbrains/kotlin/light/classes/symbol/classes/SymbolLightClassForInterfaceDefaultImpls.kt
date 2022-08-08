/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.JvmAbi

context(KtAnalysisSession)
internal class SymbolLightClassForInterfaceDefaultImpls(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    private val containingClass: SymbolLightClassBase,
    manager: PsiManager
) : SymbolLightInterfaceClass(classOrObjectSymbol, manager) {
    override fun getQualifiedName(): String? = containingClass.qualifiedName?.let { it + ".${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}" }

    override fun getName() = JvmAbi.DEFAULT_IMPLS_CLASS_NAME
    override fun getParent() = containingClass

    override fun copy() =
        SymbolLightClassForInterfaceDefaultImpls(classOrObjectSymbol, containingClass, manager)

    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()

    private val _modifierList: PsiModifierList? by lazyPub {
        val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
        SymbolLightClassModifierList(this@SymbolLightClassForInterfaceDefaultImpls, modifiers, emptyList())
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    override fun isInterface(): Boolean = false
    override fun isDeprecated(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun isEnum(): Boolean = false
    override fun hasTypeParameters(): Boolean = false
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = false

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Impossible to rename DefaultImpls")
    }

    override fun getContainingClass() = containingClass

    override fun getOwnInnerClasses() = emptyList<PsiClass>()
}

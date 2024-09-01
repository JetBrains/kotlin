/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.JvmAbi

internal class SymbolLightClassForInterfaceDefaultImpls(private val containingClass: SymbolLightClassForInterface) :
    SymbolLightClassForInterface(
        containingClass.classOrObjectDeclaration,
        containingClass.classSymbolPointer,
        containingClass.ktModule,
        containingClass.manager,
    ) {
    override fun getQualifiedName(): String? = containingClass.qualifiedName?.let { it + ".${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}" }

    override fun getName() = JvmAbi.DEFAULT_IMPLS_CLASS_NAME
    override fun getParent() = containingClass

    override fun copy() = SymbolLightClassForInterfaceDefaultImpls(containingClass)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return isEquivalentToByName(another)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightClassForInterfaceDefaultImpls && other.containingClass == containingClass

    override fun hashCode(): Int = containingClass.hashCode()

    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun computeModifierList(): PsiModifierList = SymbolLightClassModifierList(
        containingDeclaration = this,
        modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
    )

    override fun classKind(): KaClassKind = KaClassKind.CLASS

    override fun hasTypeParameters(): Boolean = false
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT

    override fun getExtendsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getExtendsList(): PsiReferenceList? = null
    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getImplementsList(): PsiReferenceList? = null

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Impossible to rename DefaultImpls")
    }

    override fun getContainingClass() = containingClass

    override fun getOwnInnerClasses() = emptyList<PsiClass>()

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    override fun acceptCallableSymbol(symbol: KaCallableSymbol): Boolean {
        return super.acceptCallableSymbol(symbol) && symbol.modality != KaSymbolModality.ABSTRACT
    }

    override fun getOwnFields(): List<PsiField> = emptyList()
}

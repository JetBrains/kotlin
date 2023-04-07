/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForScript
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightScriptMainParameter(
    private val name: String,
    private val containingMethod: SymbolLightMethodForScript,
) : SymbolLightParameterBase(containingMethod) {

    override val kotlinOrigin: KtParameter? = null
    override fun getName(): String = name

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, ktDeclaration = null, name)

    override fun getType(): PsiType {
        return PsiType.getJavaLangString(manager, resolveScope).createArrayType()
    }

    override fun equals(other: Any?): Boolean = other === this ||
            other is SymbolLightScriptMainParameter &&
            other.name == this.name &&
            other.containingMethod == this.containingMethod

    override fun hashCode(): Int = containingMethod.hashCode().times(31).plus(name.hashCode())

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
        )
    }

    override fun hasModifierProperty(name: String): Boolean = _modifierList.hasModifierProperty(name)

    override fun isVarArgs(): Boolean = false
}
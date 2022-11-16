/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList

internal class SymbolLightNoArgConstructor(
    lightMemberOrigin: LightMemberOrigin,
    containingClass: SymbolLightClassForClassOrObject,
    visibility: String,
    methodIndex: Int,
) : SymbolLightMethodBase(lightMemberOrigin, containingClass, methodIndex) {
    override fun getName(): String = containingClass.name ?: ""

    override fun isConstructor(): Boolean = true

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _identifier: PsiIdentifier by lazyPub {
        KtLightIdentifier(this, ktDeclaration = null)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun isDeprecated(): Boolean = false

    private val _modifiers: Set<String> = setOf(visibility)

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightMemberModifierList(this, _modifiers, emptyList())
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _parameterList: PsiParameterList by lazyPub {
        analyzeForLightClasses(containingClass.ktModule) {
            SymbolLightParameterList(this@SymbolLightNoArgConstructor, callableSymbol = null) {}
        }
    }

    override fun getParameterList(): PsiParameterList = _parameterList

    override fun getReturnType(): PsiType? = null

    override fun equals(other: Any?): Boolean =
        this === other || other is SymbolLightNoArgConstructor && containingClass == other.containingClass

    override fun hashCode(): Int = containingClass.hashCode()

    override fun isValid(): Boolean = super.isValid() && containingClass.isValid

    override fun isOverride(): Boolean = false
}

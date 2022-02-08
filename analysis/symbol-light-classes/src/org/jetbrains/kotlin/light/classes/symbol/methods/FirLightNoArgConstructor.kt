/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub

internal class FirLightNoArgConstructor(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    visibility: String,
    methodIndex: Int,
): FirLightMethod(lightMemberOrigin, containingClass, methodIndex) {
    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, firSymbol = null)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun isDeprecated(): Boolean = false

    private val _modifiers: Set<String> by lazyPub {
        setOf(visibility)
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightMemberModifierList(this, _modifiers, emptyList())
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _parameterList: PsiParameterList by lazyPub {
        FirLightParameterList(this, callableSymbol = null) {}
    }

    override fun getParameterList(): PsiParameterList = _parameterList

    override fun getReturnType(): PsiType? = null

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightNoArgConstructor &&
                kotlinOrigin == other.kotlinOrigin &&
                containingClass == other.containingClass)

    override fun hashCode(): Int = containingClass.hashCode()

    override fun isValid(): Boolean = super.isValid() && containingClass.isValid
}

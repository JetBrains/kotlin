/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol

internal class FirLightConstructorForSymbol(
    private val constructorSymbol: KtConstructorSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
) : FirLightMethodForSymbol(constructorSymbol, lightMemberOrigin, containingClass, methodIndex) {

    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true

    private val _annotations: List<PsiAnnotation> by lazyPub {
        constructorSymbol.computeAnnotations(this, NullabilityType.Unknown)
    }

    private val _modifiers: Set<String> by lazyPub {
        setOf(constructorSymbol.computeVisibility(isTopLevel = false))
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getReturnType(): PsiType? = null

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightConstructorForSymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 constructorSymbol == other.constructorSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
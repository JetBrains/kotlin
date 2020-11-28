/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightFieldForObjectSymbol(
    private val objectSymbol: KtClassOrObjectSymbol,
    containingClass: KtLightClass,
    lightMemberOrigin: LightMemberOrigin?,
) : FirLightField(containingClass, lightMemberOrigin) {

    override val kotlinOrigin: KtDeclaration? = objectSymbol.psi as? KtDeclaration

    private val _name = if (objectSymbol.classKind == KtClassKind.COMPANION_OBJECT) objectSymbol.name.asString() else "INSTANCE"
    override fun getName(): String = _name

    private val _modifierList: PsiModifierList by lazyPub {
        val modifiers = setOf(objectSymbol.computeVisibility(isTopLevel = false), PsiModifier.STATIC, PsiModifier.FINAL)
        val notNullAnnotation = FirLightSimpleAnnotation("org.jetbrains.annotations.NotNull", this)
        FirLightClassModifierList(this, modifiers, listOf(notNullAnnotation))
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    private val _type: PsiType by lazyPub {
        objectSymbol.typeForClassSymbol(this@FirLightFieldForObjectSymbol)
    }

    override fun getType(): PsiType = _type

    override fun getInitializer(): PsiExpression? = null //TODO

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightFieldForObjectSymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 objectSymbol == other.objectSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
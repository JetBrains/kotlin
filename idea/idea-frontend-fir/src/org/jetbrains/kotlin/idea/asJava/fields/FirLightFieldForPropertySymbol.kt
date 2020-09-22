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
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightFieldForPropertySymbol(
    private val propertySymbol: KtPropertySymbol,
    containingClass: FirLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    isTopLevel: Boolean
) : FirLightField(containingClass, lightMemberOrigin) {

    override val kotlinOrigin: KtDeclaration? = propertySymbol.psi as? KtDeclaration

    private val _returnedType: PsiType by lazyPub {
        propertySymbol.type.asPsiType(
            propertySymbol,
            this@FirLightFieldForPropertySymbol,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )
    }

    override fun getType(): PsiType = _returnedType

    private val _name = propertySymbol.name.asString()
    override fun getName(): String = _name

    private val _modifierList: PsiModifierList by lazyPub {

        val basicModifiers = propertySymbol.computeModalityForMethod(isTopLevel)
        val modifiers = if (!propertySymbol.hasAnnotation("kotlin.jvm.JvmField"))
            basicModifiers + PsiModifier.PRIVATE + PsiModifier.FINAL
        else if (propertySymbol.isVal) basicModifiers + PsiModifier.FINAL else basicModifiers

        FirLightClassModifierList(this, modifiers, emptyList())
    }

    override fun getModifierList(): PsiModifierList? = _modifierList


    override fun getInitializer(): PsiExpression? = null //TODO

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightFieldForPropertySymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 propertySymbol == other.propertySymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
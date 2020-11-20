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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightFieldForPropertySymbol(
    private val propertySymbol: KtPropertySymbol,
    containingClass: FirLightClassBase,
    lightMemberOrigin: LightMemberOrigin?,
    isTopLevel: Boolean,
    forceStatic: Boolean = false
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

        val modifiersFromSymbol = propertySymbol.computeModalityForMethod(isTopLevel = isTopLevel, isOverride = false)

        val basicModifiers = modifiersFromSymbol.add(
            what = PsiModifier.STATIC,
            `if` = forceStatic
        )

        val isJvmField = propertySymbol.hasJvmFieldAnnotation()

        val visibility =
            if (isJvmField) propertySymbol.computeVisibility(isTopLevel = false) else PsiModifier.PRIVATE

        val modifiersWithVisibility = basicModifiers + visibility

        val modifiers = modifiersWithVisibility.add(
            what = PsiModifier.FINAL,
            `if` = !isJvmField || propertySymbol.isVal
        )

        val annotations = propertySymbol.computeAnnotations(
            parent = this,
            nullability = propertySymbol.type.getTypeNullability(propertySymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE),
            annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD,
        )

        FirLightClassModifierList(this, modifiers, annotations)
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
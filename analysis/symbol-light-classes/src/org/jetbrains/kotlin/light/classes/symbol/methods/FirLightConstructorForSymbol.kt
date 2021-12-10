/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol

internal class FirLightConstructorForSymbol(
    private val constructorSymbol: KtConstructorSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
) : FirLightMethodForSymbol(constructorSymbol, lightMemberOrigin, containingClass, methodIndex) {

    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _annotations: List<PsiAnnotation> by lazyPub {
        constructorSymbol.computeAnnotations(
            parent = this,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = null,
        )
    }

    private val _isDeprecated: Boolean by lazyPub {
        constructorSymbol.hasDeprecatedAnnotation()
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _modifiers: Set<String> by lazyPub {
        // FIR treats an enum entry as an anonymous object w/ its own ctor (not default one).
        // On the other hand, FE 1.0 doesn't add anything; then ULC adds default ctor w/ package local visibility.
        // Technically, an enum entry should not be instantiated anywhere else, and thus FIR's modeling makes sense.
        // But, to be backward compatible, we manually force the visibility of enum entry ctor to be package private.
        if (containingClass is FirLightClassForEnumEntry)
            setOf(PsiModifier.PACKAGE_LOCAL)
        else
            setOf(constructorSymbol.toPsiVisibilityForMember(isTopLevel = false))
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightMemberModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getReturnType(): PsiType? = null

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightConstructorForSymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 constructorSymbol == other.constructorSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && constructorSymbol.isValid()
}

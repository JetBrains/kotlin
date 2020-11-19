/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.asJava.elements.FirLightTypeParameterListForSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.isUnit
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

internal class FirLightSimpleMethodForSymbol(
    private val functionSymbol: KtFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
    isTopLevel: Boolean,
    argumentsSkipMask: BitSet? = null
) : FirLightMethodForSymbol(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask
) {

    private val _name: String by lazyPub {
        functionSymbol.computeJvmMethodName(functionSymbol.name.asString(), containingClass)
    }

    override fun getName(): String = _name

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            FirLightTypeParameterListForSymbol(
                owner = this,
                symbolWithTypeParameterList = functionSymbol,
                innerShiftCount = 0
            )
        }
    }

    override fun hasTypeParameters(): Boolean =
        functionSymbol.typeParameters.isNotEmpty()

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> =
        _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private val _annotations: List<PsiAnnotation> by lazyPub {
        val needUnknownNullability =
            isVoidReturnType || (_visibility == PsiModifier.PRIVATE)

        val nullability = if (needUnknownNullability)
            NullabilityType.Unknown
        else functionSymbol.type.getTypeNullability(
            context = functionSymbol,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )

        functionSymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    private val _visibility: String by lazyPub {
        functionSymbol.isOverride.ifTrue {
            (containingClass as? FirLightClassForSymbol)
                ?.tryGetEffectiveVisibility(functionSymbol)
                ?.toPsiVisibility(isTopLevel)
        } ?: functionSymbol.computeVisibility(isTopLevel = isTopLevel)
    }

    private val _modifiers: Set<String> by lazyPub {

        if (functionSymbol.hasInlineOnlyAnnotation()) return@lazyPub setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val finalModifier = kotlinOrigin?.hasModifier(KtTokens.FINAL_KEYWORD) == true

        val modifiers = functionSymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = !finalModifier && functionSymbol.isOverride
        ) + _visibility

        modifiers.add(
            what = PsiModifier.STATIC,
            `if` = functionSymbol.hasJvmStaticAnnotation()
        )
    }

    private val _isDeprecated: Boolean by lazyPub {
        functionSymbol.hasDeprecatedAnnotation()
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val isVoidReturnType: Boolean
        get() = functionSymbol.type.run {
            isUnit && nullabilityType != NullabilityType.Nullable
        }

    private val _returnedType: PsiType by lazyPub {
        if (isVoidReturnType) return@lazyPub PsiType.VOID
        functionSymbol.asPsiType(this@FirLightSimpleMethodForSymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other || (other is FirLightSimpleMethodForSymbol && functionSymbol == other.functionSymbol)

    override fun hashCode(): Int = functionSymbol.hashCode()
}
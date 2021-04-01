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
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

internal class FirLightSimpleMethodForSymbol(
    private val functionSymbol: KtFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
    private val isTopLevel: Boolean,
    argumentsSkipMask: BitSet? = null,
    private val suppressStatic: Boolean = false
) : FirLightMethodForSymbol(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
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

    private fun computeAnnotations(isPrivate: Boolean): List<PsiAnnotation> {
        val nullability = if (isVoidReturnType || isPrivate)
            NullabilityType.Unknown
        else functionSymbol.annotatedType.type.getTypeNullability(
            context = functionSymbol,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )

        return functionSymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    private fun computeModifiers(): Set<String> {

        if (functionSymbol.hasInlineOnlyAnnotation()) return setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val finalModifier = kotlinOrigin?.hasModifier(KtTokens.FINAL_KEYWORD) == true

        val modifiers = mutableSetOf<String>()

        functionSymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = !finalModifier && functionSymbol.isOverride,
            result = modifiers
        )

        val visibility: String = functionSymbol.isOverride.ifTrue {
            (containingClass as? FirLightClassForSymbol)
                ?.tryGetEffectiveVisibility(functionSymbol)
                ?.toPsiVisibilityForMember(isTopLevel)
        } ?: functionSymbol.toPsiVisibilityForMember(isTopLevel = isTopLevel)

        modifiers.add(visibility)

        if (!suppressStatic && functionSymbol.hasJvmStaticAnnotation()) {
            modifiers.add(PsiModifier.STATIC)
        }
        if (functionSymbol.hasAnnotation("kotlin/jvm/Strictfp", null)) {
            modifiers.add(PsiModifier.STRICTFP)
        }
        if (functionSymbol.hasAnnotation("kotlin/jvm/Synchronized", null)) {
            modifiers.add(PsiModifier.SYNCHRONIZED)
        }

        return modifiers
    }

    private val _isDeprecated: Boolean by lazyPub {
        functionSymbol.hasDeprecatedAnnotation()
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _modifierList: PsiModifierList by lazyPub {
        val modifiers = computeModifiers()
        val annotations = computeAnnotations(modifiers.contains(PsiModifier.PRIVATE))
        FirLightClassModifierList(this, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val isVoidReturnType: Boolean
        get() = functionSymbol.annotatedType.type.run {
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

    override fun isValid(): Boolean = super.isValid() && functionSymbol.isValid()
}
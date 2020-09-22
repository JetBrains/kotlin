/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
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
        functionSymbol.name.asString()
    }

    override fun getName(): String = _name

    private val _annotations: List<PsiAnnotation> by lazyPub {
        functionSymbol.computeAnnotations(this, functionSymbol.type.nullabilityType)
    }

    private val _modifiers: Set<String> by lazyPub {

        val isInlineOnly =
            _annotations.any { it.qualifiedName == "kotlin.internal.InlineOnly" }

        if (isInlineOnly) return@lazyPub setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val modifiers = functionSymbol.computeModalityForMethod(isTopLevel = isTopLevel) +
                functionSymbol.computeVisibility(isTopLevel = isTopLevel)

        val isJvmStatic =
            _annotations.any { it.qualifiedName == "kotlin.jvm.JvmStatic" }

        if (isJvmStatic) return@lazyPub modifiers + PsiModifier.STATIC

        modifiers
    }


    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _returnedType: PsiType? by lazyPub {
        functionSymbol.asPsiType(this@FirLightSimpleMethodForSymbol)
    }

    override fun getReturnType(): PsiType? = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSimpleMethodForSymbol &&
                 kotlinOrigin == other.kotlinOrigin &&
                 functionSymbol == other.functionSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
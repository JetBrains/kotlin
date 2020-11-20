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
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.isUnit
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
        functionSymbol.getJvmNameFromAnnotation(null) ?: functionSymbol.name.asString()
    }

    override fun getName(): String = _name

    private val _annotations: List<PsiAnnotation> by lazyPub {

        val nullability = if (functionSymbol.type.isUnit) NullabilityType.Unknown else functionSymbol.type.getTypeNullability(
            functionSymbol,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )

        functionSymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    private val _modifiers: Set<String> by lazyPub {

        if (functionSymbol.hasInlineOnlyAnnotation()) return@lazyPub setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val modifiers = functionSymbol.computeModalityForMethod(isTopLevel = isTopLevel, functionSymbol.isOverride) +
                functionSymbol.computeVisibility(isTopLevel = isTopLevel)

        if (functionSymbol.hasJvmStaticAnnotation()) return@lazyPub modifiers + PsiModifier.STATIC

        modifiers
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _returnedType: PsiType by lazyPub {
        if (functionSymbol.type.isUnit) return@lazyPub PsiType.VOID
        functionSymbol.asPsiType(this@FirLightSimpleMethodForSymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSimpleMethodForSymbol &&
                        kotlinOrigin == other.kotlinOrigin &&
                        functionSymbol == other.functionSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.hasBody

internal class SymbolLightMemberModifierList<T : KtLightMember<*>> : SymbolLightModifierList<T> {
    constructor(
        containingDeclaration: T,
        initialValue: Map<String, Boolean> = emptyMap(),
        lazyModifiersComputer: LazyModifiersComputer,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(containingDeclaration, initialValue, lazyModifiersComputer, annotationsComputer)

    constructor(
        containingDeclaration: T,
        staticModifiers: Set<String>,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(containingDeclaration, staticModifiers, annotationsComputer)

    override fun hasModifierProperty(name: String): Boolean = when {
        name == PsiModifier.ABSTRACT && isImplementationInInterface() -> false
        // Pretend this method behaves like a `default` method
        name == PsiModifier.DEFAULT && isImplementationInInterface() -> true
        // TODO: FINAL && isPossiblyAffectedByAllOpen
        else -> super.hasModifierProperty(name)
    }

    private fun isImplementationInInterface(): Boolean {
        return owner.containingClass.isInterface && owner is SymbolLightMethodBase && owner.kotlinOrigin?.hasBody() == true
    }

    override fun hasExplicitModifier(name: String): Boolean {
        // Kotlin methods can't be truly default atm, that way we can avoid being reported on by diagnostics, namely UAST
        return if (name == PsiModifier.DEFAULT) false else super.hasExplicitModifier(name)
    }

    private inline fun <R> getTextVariantFromModifierListOfPropertyAccessorIfNeeded(
        retriever: (KtModifierList) -> R
    ): R? {
        val auxiliaryOrigin = (owner as? KtLightMember<*>)?.lightMemberOrigin?.auxiliaryOriginalElement
        return (auxiliaryOrigin as? KtPropertyAccessor)?.modifierList?.let(retriever)
    }

    override fun getText(): String {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getText)
            ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getTextOffset)
            ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getTextRange)
            ?: super.getTextRange()
    }
}

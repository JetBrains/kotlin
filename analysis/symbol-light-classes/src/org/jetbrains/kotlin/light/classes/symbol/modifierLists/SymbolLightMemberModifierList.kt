/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.light.classes.symbol.invalidAccess
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.hasBody

internal class SymbolLightMemberModifierList<T : KtLightMember<*>>(
    containingDeclaration: T,
    private val modifiers: Set<String>,
    private val annotations: List<PsiAnnotation>
) : SymbolLightModifierList<T>(containingDeclaration) {
    init {
        annotations.forEach {
            (it as? KtLightElementBase)?.parent = this
        }
    }

    override fun hasModifierProperty(name: String): Boolean {
        return when {
            name == PsiModifier.ABSTRACT && isImplementationInInterface() -> false
            // Pretend this method behaves like a `default` method
            name == PsiModifier.DEFAULT && isImplementationInInterface() -> true
            // TODO: FINAL && isPossiblyAffectedByAllOpen
            else -> {
                name in modifiers
            }
        }
    }

    private fun isImplementationInInterface(): Boolean {
        return owner.containingClass.isInterface && owner is SymbolLightMethodBase && owner.kotlinOrigin?.hasBody() == true
    }

    override fun hasExplicitModifier(name: String): Boolean {
        // Kotlin methods can't be truly default atm, that way we can avoid being reported on by diagnostics, namely UAST
        return if (name == PsiModifier.DEFAULT) false else super.hasExplicitModifier(name)
    }

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = invalidAccess()

    override fun getAnnotations(): Array<out PsiAnnotation> = annotations.toTypedArray()

    override fun findAnnotation(qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

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

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}

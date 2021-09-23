/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.psi.psiUtil.hasBody

internal class FirLightMemberModifierList<T : KtLightMember<*>>(
    containingDeclaration: T,
    private val modifiers: Set<String>,
    private val annotations: List<PsiAnnotation>
) : FirLightModifierList<T>(containingDeclaration) {
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
        return owner.containingClass.isInterface && owner is FirLightMethod && owner.kotlinOrigin?.hasBody() == true
    }

    override fun hasExplicitModifier(name: String): Boolean {
        // Kotlin methods can't be truly default atm, that way we can avoid being reported on by diagnostics, namely UAST
        return if (name == PsiModifier.DEFAULT) false else super.hasExplicitModifier(name)
    }

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = invalidAccess()

    override fun getAnnotations(): Array<out PsiAnnotation> = annotations.toTypedArray()
    override fun findAnnotation(qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}

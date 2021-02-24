/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class FirLightClassModifierList<T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    containingDeclaration: T,
    private val modifiers: Set<String>,
    private val annotations: List<PsiAnnotation>
) : FirLightModifierList<T>(containingDeclaration) {
    override fun hasModifierProperty(name: String): Boolean = name in modifiers

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = invalidAccess()

    override fun getAnnotations(): Array<out PsiAnnotation> = annotations.toTypedArray()
    override fun findAnnotation(qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
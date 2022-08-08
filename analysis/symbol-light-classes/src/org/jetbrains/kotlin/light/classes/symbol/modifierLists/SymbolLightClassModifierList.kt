/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.invalidAccess
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal class SymbolLightClassModifierList<T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    containingDeclaration: T,
    private val modifiers: Set<String>,
    private val annotations: List<PsiAnnotation>
) : SymbolLightModifierList<T>(containingDeclaration) {
    init {
        annotations.forEach {
            (it as? KtLightElementBase)?.parent = this
        }
    }

    override fun hasModifierProperty(name: String): Boolean = name in modifiers

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = invalidAccess()

    override fun getAnnotations(): Array<out PsiAnnotation> = annotations.toTypedArray()
    override fun findAnnotation(qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = kotlinOrigin.hashCode()
}
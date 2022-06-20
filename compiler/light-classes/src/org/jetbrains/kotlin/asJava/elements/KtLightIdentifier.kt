/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

abstract class KtLightIdentifierBase(
    private val lightOwner: PsiElement,
    text: String?
) : LightIdentifier(lightOwner.manager, text), PsiElementWithOrigin<PsiElement> {
    override fun isPhysical() = true
    override fun getParent() = lightOwner
    override fun getContainingFile() = lightOwner.containingFile
    override fun getTextRange() = origin?.textRange ?: TextRange.EMPTY_RANGE
    override fun getTextOffset(): Int = origin?.textOffset ?: -1
}

open class KtLightIdentifier(
    lightOwner: PsiElement,
    private val ktDeclaration: KtDeclaration?
) : KtLightIdentifierBase(lightOwner, ktDeclaration?.name) {
    override val origin: PsiElement?
        get() = when (ktDeclaration) {
            is KtSecondaryConstructor -> ktDeclaration.getConstructorKeyword()
            is KtPrimaryConstructor -> ktDeclaration.getConstructorKeyword()
                ?: ktDeclaration.valueParameterList
                ?: ktDeclaration.containingClassOrObject?.nameIdentifier

            is KtPropertyAccessor -> ktDeclaration.namePlaceholder
            is KtNamedDeclaration -> ktDeclaration.nameIdentifier
            else -> null
        }
}

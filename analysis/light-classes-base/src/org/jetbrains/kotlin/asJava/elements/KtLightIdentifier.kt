/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

open class KtLightIdentifier @JvmOverloads constructor(
    private val lightOwner: PsiElement,
    private val ktDeclaration: KtDeclaration?,
    private val name: String? = ktDeclaration?.name,
) : LightIdentifier(lightOwner.manager, name), PsiElementWithOrigin<PsiElement> {
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

    override fun copy(): PsiElement = KtLightIdentifier(parent, ktDeclaration, name)
    override fun isPhysical(): Boolean = true
    override fun getParent(): PsiElement = lightOwner
    override fun getContainingFile(): PsiFile = lightOwner.containingFile
    override fun getTextRange(): TextRange = origin?.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset(): Int = origin?.textOffset ?: -1

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is KtLightIdentifier &&
                other.lightOwner == lightOwner &&
                other.ktDeclaration == ktDeclaration &&
                other.name == name
    }

    override fun hashCode(): Int = lightOwner.hashCode()
}

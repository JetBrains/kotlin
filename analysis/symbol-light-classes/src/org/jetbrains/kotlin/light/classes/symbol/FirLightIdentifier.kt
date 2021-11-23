/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.asJava.elements.PsiElementWithOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class FirLightIdentifier(
    private val lightOwner: PsiElement,
    private val firSymbol: KtSymbol?
) : LightIdentifier(
    lightOwner.manager,
    (firSymbol as? KtNamedSymbol)?.name?.identifierOrNullIfSpecial
), PsiCompiledElement, PsiElementWithOrigin<PsiElement> {

    override val origin: PsiElement?
        get() = when (val ktDeclaration = firSymbol?.psi) {
            is KtSecondaryConstructor -> ktDeclaration.getConstructorKeyword()
            is KtPrimaryConstructor -> ktDeclaration.getConstructorKeyword()
                ?: ktDeclaration.valueParameterList
                ?: ktDeclaration.containingClassOrObject?.nameIdentifier
            is KtPropertyAccessor -> ktDeclaration.namePlaceholder
            is KtNamedDeclaration -> ktDeclaration.nameIdentifier
            else -> null
        }

    override fun getMirror(): PsiElement? = null
    override fun isPhysical(): Boolean = true
    override fun getParent(): PsiElement = lightOwner
    override fun getContainingFile(): PsiFile = lightOwner.containingFile
    override fun getTextRange(): TextRange = origin?.textRange ?: TextRange.EMPTY_RANGE
    override fun getTextOffset(): Int = origin?.textOffset ?: -1
}

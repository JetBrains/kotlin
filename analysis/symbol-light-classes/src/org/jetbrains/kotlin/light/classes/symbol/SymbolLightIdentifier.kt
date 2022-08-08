/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifierBase
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class SymbolLightIdentifier(
    lightOwner: PsiElement,
    private val ktSymbol: KtSymbol?
) : KtLightIdentifierBase(lightOwner, (ktSymbol as? KtNamedSymbol)?.name?.identifierOrNullIfSpecial) {

    override val origin: PsiElement?
        get() = when (val ktDeclaration = ktSymbol?.psi) {
            is KtSecondaryConstructor -> ktDeclaration.getConstructorKeyword()
            is KtPrimaryConstructor -> ktDeclaration.getConstructorKeyword()
                ?: ktDeclaration.valueParameterList
                ?: ktDeclaration.containingClassOrObject?.nameIdentifier

            is KtPropertyAccessor -> ktDeclaration.namePlaceholder
            is KtNamedDeclaration -> ktDeclaration.nameIdentifier
            else -> null
        }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.fir.FirSyntaxComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.toKtPsiSourceElement

/**
 * [FirSyntaxComponent] for sessions whose sources are built by [PsiRawFirBuilder].
 */
@NoMutableState
object FirPsiSyntaxComponent : FirAbstractPsiSyntaxComponent() {
    override val parserMode: ParserMode
        get() = ParserMode.Psi

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        return psi?.getChildOfType<KtModifierList>()?.contextParameterLists?.map { it.toKtPsiSourceElement() }.orEmpty()
    }

    override fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement? {
        fun PsiElement.findContextParameterList(): KtContextParameterList? {
            return when (this) {
                is KtTypeReference -> typeElement?.findContextParameterList()
                is KtNullableType -> innerType?.findContextParameterList()
                is KtFunctionType -> contextParameterList
                else -> null
            }
        }

        return psi?.findContextParameterList()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        return (psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        return (psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        return psi?.getExplicitReceiverOfDotQualified()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        return psi?.findDescendantOfType<KtParameter> { it.nameAsName == name }?.nameIdentifier?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        var parent = psi?.parent
        while (parent != null) {
            if (parent is KtStringTemplateExpression) return parent.children.size
            parent = parent.parent
        }
        return null
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        return (psi as? KtLambdaExpression)?.isTrailingLambdaOnNewLine == true
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.toKtPsiSourceElement

@NoMutableState
object FirPsiSyntaxComponent : FirSyntaxComponent<KtPsiSourceElement>() {
    override val parserMode: ParserMode
        get() = Psi

    override fun KtSourceElement.unwrap(): KtPsiSourceElement {
        return this as? KtPsiSourceElement ?: unwrapError(this@unwrap)
    }

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        return unwrap().psi.getChildOfType<KtModifierList>()?.contextParameterLists?.map { it.toKtPsiSourceElement() }.orEmpty()
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

        return unwrap().psi.findContextParameterList()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        return (unwrap().psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        return (unwrap().psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        return unwrap().psi.getExplicitReceiverOfDotQualified()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        return unwrap().psi.findDescendantOfType<KtParameter> { it.nameAsName == name }?.nameIdentifier?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        var parent = unwrap().psi.parent
        while (parent != null) {
            if (parent is KtStringTemplateExpression) return parent.children.size
            parent = parent.parent
        }
        return null
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        return (unwrap().psi as? KtLambdaExpression)?.isTrailingLambdaOnNewLine == true
    }

    override fun KtSourceElement.isInConstructorCallee(): Boolean {
        return (unwrap().psi as? KtTypeReference)?.parent is KtConstructorCalleeExpression
    }

    override fun KtSourceElement.isInTypeConstraint(): Boolean {
        return PsiTreeUtil.getParentOfType(unwrap().psi, KtTypeConstraint::class.java, KtTypeParameter::class.java) is KtTypeConstraint
    }

    override fun KtSourceElement.getRawIdentifier(): CharSequence? = unwrap().psi.getRawIdentifier()

    private fun PsiElement.getRawIdentifier(): CharSequence? = when (this) {
        is KtNameReferenceExpression -> getReferencedNameElement().node.chars
        is KtTypeProjection -> typeReference?.typeElement?.text
        is LeafPsiElement if elementType == KtTokens.IDENTIFIER -> chars
        is KtQualifiedExpression -> selectorExpression?.getRawIdentifier()
        is KtImportAlias -> name
        else -> null
    }

    override fun KtSourceElement.getRawName(): String? {
        return (unwrap().psi as? PsiNameIdentifierOwner)?.nameIdentifier?.text
    }

    override fun KtSourceElement.isRedundantNullable(): Boolean {
        val typeReference = unwrap().psi as? KtTypeReference ?: return false
        val typeElement = typeReference.typeElement as? KtNullableType ?: return false
        return typeElement.innerType is KtNullableType
    }

    override fun KtSourceElement.hasEnumEntryBody(): Boolean {
        return (unwrap().psi as? KtEnumEntry)?.body != null
    }

    override fun KtSourceElement.hasEnumEntryInitializer(): Boolean {
        return (unwrap().psi as? KtEnumEntry)?.initializerList != null
    }
}

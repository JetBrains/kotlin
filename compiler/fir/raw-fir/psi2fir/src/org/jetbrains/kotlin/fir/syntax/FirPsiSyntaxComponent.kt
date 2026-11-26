/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLiteralPrefixAndSuffix
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isStatementContainer
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentLhsIfUnwrappable
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.syntax.BasicSyntaxElementTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.ParseTreeKind
import org.jetbrains.kotlin.syntax.PsiParseTreeKind
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.elementType
import org.jetbrains.kotlin.syntax.syntaxId
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

@NoMutableState
object FirPsiSyntaxComponent : FirSyntaxComponent<KtPsiSourceElement>() {
    override val parseTreeKind: ParseTreeKind
        get() = PsiParseTreeKind

    override fun KtSourceElement.unwrap(): KtPsiSourceElement {
        return this as? KtPsiSourceElement ?: unwrapError(this@unwrap)
    }

    override val SyntaxId.name: String
        get() {
            @OptIn(KtImplementationDetail::class)
            return elementType().toString()
        }

    override fun KtSourceElement.forEachChildMatching(
        depth: Int,
        reverse: Boolean,
        isTargetType: SyntaxIdPredicate,
        processChild: (KtSourceElement) -> Boolean,
    ) {
        forEachChildMatching(
            unwrap().psi, depth, reverse, isTargetType,
            getSyntaxId = { @OptIn(KtImplementationDetail::class) (it.elementType?.syntaxId() ?: BasicSyntaxElementTypeIds.NO_ID) },
            getChildren = { it.allChildren.toList() },
        ) {
            processChild(it.toKtPsiSourceElement())
        }
    }

    override fun KtSourceElement.extractModifierList(): FirModifierList? {
        val source = unwrap()
        // todo this code is buggy. psi for fake declarations (e.g. ImplicitConstructor, EnumGeneratedDeclaration) means a completely different thing
        //  KT-63751
        val modifierListOwner = source.psi as? KtModifierListOwner

        // The check is required in the Analysis API mode as in this case property accessor
        // has the containing property as a source
        if (source.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor && modifierListOwner is KtProperty) {
            errorWithAttachment("Don't request modifiers on fake PSI of delegated property accessors, it's not the right PSI") {
                withPsiEntry("property", modifierListOwner)
            }
        }

        val modifierList = modifierListOwner?.modifierList ?: return null
        val modifiers = mutableListOf<FirModifier>()
        val annotationSources = mutableListOf<KtSourceElement>()
        for (node in modifierList.node.children()) {
            when (val elementType = node.elementType) {
                is KtModifierKeywordToken -> modifiers += FirModifier(elementType.tokenId, node.psi.toKtPsiSourceElement())
                KtNodeTypes.ANNOTATION_ENTRY -> annotationSources += KtRealPsiSourceElement(node.psi)
            }
        }
        return FirModifierList(modifiers, annotationSources)
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
        // Not `KtValVarKeywordOwner.valOrVarKeyword`: for a destructuring declaration entry, it returns the keyword of the whole declaration
        return unwrap().psi.node?.findChildByType(KtTokens.VAL_VAR)?.psi?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        return unwrap().psi.getExplicitReceiverOfDotQualified()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.hasUnwrappableAsAssignmentLhs(): Boolean {
        return unwrap().psi.getAssignmentLhsIfUnwrappable() != null
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

    override fun KtSourceElement.isAnnotatedBinaryExpressionStatement(): Boolean {
        var current = unwrap().psi as? KtBinaryExpression ?: return false

        if (current.left !is KtAnnotatedExpression) return false

        var parent = current.parent

        while (parent is KtBinaryExpression) {
            if (parent.left != current) {
                return false
            }

            current = parent
            parent = parent.parent
        }

        return isStatementContainer(parent)
    }

    override fun KtSourceElement.findDelegatedSuperTypeEntrySource(): KtSourceElement? {
        return (unwrap().psi.parent as? KtDelegatedSuperTypeEntry)?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.forEachWhenConditionOperation(process: (expression: KtSourceElement, operation: SyntaxId) -> Unit) {
        val whenExpression = unwrap().psi as? KtWhenExpression ?: return
        if (whenExpression.subjectExpression == null && whenExpression.subjectVariable == null) return
        for (entry in whenExpression.entries) {
            for (condition in entry.conditions) {
                val rawExpression = when (condition) {
                    is KtWhenConditionWithExpression -> condition.expression
                    is KtWhenConditionInRange -> condition.rangeExpression
                    else -> null
                } ?: continue
                if (rawExpression is KtParenthesizedExpression) continue
                val operation = when (val expression = KtPsiUtil.safeDeparenthesize(rawExpression)) {
                    is KtIsExpression -> KotlinSyntaxNodeTypeIds.IS_EXPRESSION
                    is KtBinaryExpression -> {
                        @OptIn(KtImplementationDetail::class)
                        expression.operationToken.syntaxId()
                    }
                    else -> continue
                }
                process(rawExpression.toKtPsiSourceElement(), operation)
            }
        }
    }

    override fun KtSourceElement.findUnderscoreTypeArgumentAnnotationSources(typeArgument: KtSourceElement): List<KtSourceElement> {
        val typeReference = (typeArgument.unwrap().psi as? KtTypeProjection)?.typeReference ?: return emptyList()
        if (!typeReference.isPlaceholder) return emptyList()
        return typeReference.annotationEntries.map { it.toKtPsiSourceElement() }
    }

    /**
     * Reaches the neighbors of the literal with [prevLeaf], which climbs the tree back up once per literal (KT-88399).
     * Its cost is unclear, but the PSI path is not performance-critical, so it is acceptable here.
     */
    override fun KtSourceElement.findProhibitedLiteralPrefixSource(): KtSourceElement? {
        return findProhibitedLiteralAffix { it.prevLeaf() }
    }

    /**
     * See [findProhibitedLiteralPrefixSource].
     */
    override fun KtSourceElement.findProhibitedLiteralSuffixSource(): KtSourceElement? {
        return findProhibitedLiteralAffix { it.nextLeaf() }
    }

    @OptIn(KtImplementationDetail::class)
    private inline fun KtSourceElement.findProhibitedLiteralAffix(getAffix: (PsiElement) -> PsiElement?): KtSourceElement? {
        val psi = unwrap().psi
        if (psi.node?.elementType !in KtLiteralPrefixAndSuffix.relevantLiteralTypes) return null
        val affix = getAffix(psi) ?: return null
        return runIf(KtLiteralPrefixAndSuffix.isProhibitedPrefixOrSuffix(affix.node.elementType)) { affix.toKtPsiSourceElement() }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.light

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterASTTokenNode
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.LightTreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

/**
 * Helpers for reading the same syntactic facts from a [LighterAST] that the AST-based stub factories read from PSI.
 *
 * Every function here mirrors a specific PSI implementation (referenced in its documentation); the two must produce
 * identical answers because stubs built from the light tree during indexing have to match stubs built from the AST.
 */

internal fun LighterAST.firstChildOfType(node: LighterASTNode, type: IElementType): LighterASTNode? =
    LightTreeUtil.firstChildOfType(this, node, type)

internal fun LighterAST.firstChildOfType(node: LighterASTNode, types: TokenSet): LighterASTNode? =
    LightTreeUtil.firstChildOfType(this, node, types)

/** The exact source text of [node], including whitespace and comments inside it. Mirrors `PsiElement.getText`. */
internal fun LighterAST.nodeText(node: LighterASTNode): String =
    LightTreeUtil.toFilteredString(this, node, null)

/**
 * Composite node types that do **not** correspond to a [org.jetbrains.kotlin.psi.KtExpression].
 *
 * Everything else composite the Kotlin parser produces is a `KtExpression` (declarations included, since
 * `KtDeclaration` extends `KtExpression`). Mirrors the reasoning of `org.jetbrains.kotlin.fir.analysis.isExpression`
 * in `light-tree2fir`.
 */
private val NON_EXPRESSION_TYPES: TokenSet = TokenSet.create(
    TokenType.ERROR_ELEMENT,
    KDocTokens.KDOC,
    KtNodeTypes.CLASS_BODY,
    KtNodeTypes.COMPANION_BLOCK,
    KtNodeTypes.INITIALIZER_LIST,
    KtNodeTypes.VALUE_PARAMETER_LIST,
    KtNodeTypes.CONTEXT_PARAMETER_LIST,
    KtNodeTypes.CONTEXT_RECEIVER,
    KtNodeTypes.TYPE_PARAMETER_LIST,
    KtNodeTypes.TYPE_CONSTRAINT_LIST,
    KtNodeTypes.TYPE_CONSTRAINT,
    KtNodeTypes.SUPER_TYPE_LIST,
    KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY,
    KtNodeTypes.SUPER_TYPE_CALL_ENTRY,
    KtNodeTypes.SUPER_TYPE_ENTRY,
    KtNodeTypes.MODIFIER_LIST,
    KtNodeTypes.ANNOTATION,
    KtNodeTypes.ANNOTATION_ENTRY,
    KtNodeTypes.ANNOTATION_TARGET,
    KtNodeTypes.TYPE_REFERENCE,
    KtNodeTypes.USER_TYPE,
    KtNodeTypes.DYNAMIC_TYPE,
    KtNodeTypes.FUNCTION_TYPE,
    KtNodeTypes.FUNCTION_TYPE_RECEIVER,
    KtNodeTypes.NULLABLE_TYPE,
    KtNodeTypes.INTERSECTION_TYPE,
    KtNodeTypes.UNION_TYPE,
    KtNodeTypes.TYPE_PROJECTION,
    KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY,
    KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY,
    KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY,
    KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
    KtNodeTypes.STRING_INTERPOLATION_PREFIX,
    KtNodeTypes.TYPE_ARGUMENT_LIST,
    KtNodeTypes.VALUE_ARGUMENT_LIST,
    KtNodeTypes.VALUE_ARGUMENT,
    KtNodeTypes.CONTRACT_EFFECT_LIST,
    KtNodeTypes.CONTRACT_EFFECT,
    KtNodeTypes.LAMBDA_ARGUMENT,
    KtNodeTypes.VALUE_ARGUMENT_NAME,
    KtNodeTypes.PACKAGE_DIRECTIVE,
    KtNodeTypes.FILE_ANNOTATION_LIST,
    KtNodeTypes.IMPORT_LIST,
    KtNodeTypes.IMPORT_DIRECTIVE,
    KtNodeTypes.IMPORT_ALIAS,
    KtNodeTypes.PROPERTY_DELEGATE,
    KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL,
    KtNodeTypes.LABEL_QUALIFIER,
)

/**
 * Whether [node] corresponds to a [org.jetbrains.kotlin.psi.KtExpression] in the PSI tree: a composite node whose
 * type is not one of the known non-expression composites. Tokens (including collapsed lazy-parseable ones such as
 * KDoc) are leaves and never expressions.
 */
internal fun isExpression(node: LighterASTNode): Boolean {
    if (node is LighterASTTokenNode) return false
    val type = node.tokenType
    return type !in NON_EXPRESSION_TYPES && type !is IFileElementType
}

/** All children of [node] which correspond to [org.jetbrains.kotlin.psi.KtExpression]s, in source order. */
internal fun LighterAST.expressionChildren(node: LighterASTNode): List<LighterASTNode> =
    getChildren(node).filter { isExpression(it) }

/** The first child of [node] which corresponds to a [org.jetbrains.kotlin.psi.KtExpression]. */
internal fun LighterAST.firstExpressionChild(node: LighterASTNode): LighterASTNode? =
    getChildren(node).firstOrNull { isExpression(it) }

/**
 * The first expression following the `=` token among the children of [node], or `null` if there is no `=` or no
 * expression after it. Mirrors `KtDeclarationWithInitializer.getInitializer` implementations, which take the next
 * sibling of type `KtExpression` after the equals token.
 */
internal fun LighterAST.initializerAfterEq(node: LighterASTNode): LighterASTNode? {
    var seenEq = false
    for (child in getChildren(node)) {
        when {
            !seenEq && child.tokenType == KtTokens.EQ -> seenEq = true
            seenEq && isExpression(child) -> return child
        }
    }
    return null
}

/** The name identifier token of a declaration, mirroring `KtNamedDeclarationStub.getNameIdentifier`. */
internal fun LighterAST.nameIdentifier(declaration: LighterASTNode): LighterASTNode? =
    firstChildOfType(declaration, KtTokens.IDENTIFIER)

/** The declared name with backticks stripped, mirroring `KtNamedDeclarationStub.getName`. */
internal fun LighterAST.declarationName(declaration: LighterASTNode): String? =
    nameIdentifier(declaration)?.let { KtPsiUtil.unquoteIdentifier(nodeText(it)) }

/**
 * The name of an object declaration, mirroring `KtObjectDeclaration.getName`: a non-top-level companion object
 * without an identifier is named `Companion`.
 */
internal fun LighterAST.objectName(objectDeclaration: LighterASTNode): String? {
    declarationName(objectDeclaration)?.let { return it }

    if (hasModifier(objectDeclaration, KtTokens.COMPANION_KEYWORD) && !isTopLevelDeclaration(objectDeclaration)) {
        return "Companion"
    }

    return null
}

/** The name of a named declaration node of any type, dispatching to the object special case when needed. */
internal fun LighterAST.anyDeclarationName(declaration: LighterASTNode): String? = when (declaration.tokenType) {
    KtNodeTypes.OBJECT_DECLARATION -> objectName(declaration)
    else -> declarationName(declaration)
}

/** Whether the direct tree parent of [node] is the file root, mirroring `isKtFile(getParent())` checks. */
internal fun LighterAST.isTopLevelDeclaration(node: LighterASTNode): Boolean =
    getParent(node)?.tokenType is IFileElementType

internal fun LighterAST.modifierList(owner: LighterASTNode): LighterASTNode? =
    firstChildOfType(owner, KtNodeTypes.MODIFIER_LIST)

/** Mirrors `KtModifierListOwner.hasModifier`: the declaration's own modifier list contains the keyword token. */
internal fun LighterAST.hasModifier(owner: LighterASTNode, modifier: KtModifierKeywordToken): Boolean {
    val modifierList = modifierList(owner) ?: return false
    return firstChildOfType(modifierList, modifier) != null
}

/**
 * The referenced name of a simple name expression node (`REFERENCE_EXPRESSION`, `OPERATION_REFERENCE`, etc.),
 * mirroring `KtSimpleNameExpressionImpl.getReferencedNameImpl`: the text of the name element with backticks (and a
 * leading `$` field reference prefix) handled.
 */
internal fun LighterAST.referencedName(simpleNameExpression: LighterASTNode, nameElementTypes: TokenSet): String {
    val nameElement = firstChildOfType(simpleNameExpression, nameElementTypes) ?: simpleNameExpression
    return KtPsiUtil.unquoteIdentifierOrFieldReference(nodeText(nameElement))
}

/** Mirrors `KtNameReferenceExpression.getReferencedNameElement` child types. */
internal val NAME_REFERENCE_NAME_ELEMENT_TYPES: TokenSet =
    TokenSet.create(KtTokens.IDENTIFIER, KtTokens.THIS_KEYWORD, KtTokens.SUPER_KEYWORD)

/** Whether the subtree of [node] contains a parser error element, mirroring `PsiTreeUtil.hasErrorElements`. */
internal fun LighterAST.hasErrorElements(node: LighterASTNode): Boolean {
    if (node.tokenType == TokenType.ERROR_ELEMENT) return true
    return getChildren(node).any { hasErrorElements(it) }
}

/**
 * Unwraps parenthesized, labeled, and annotated expressions, mirroring
 * `KtPsiUtil.deparenthesizeOnce`-based `unwrapParenthesesLabelsAndAnnotations`.
 */
internal fun LighterAST.unwrapParenthesesLabelsAndAnnotations(node: LighterASTNode): LighterASTNode {
    var current = node
    while (true) {
        current = when (current.tokenType) {
            KtNodeTypes.PARENTHESIZED,
            KtNodeTypes.LABELED_EXPRESSION,
            KtNodeTypes.ANNOTATED_EXPRESSION,
                -> firstExpressionChild(current) ?: return current
            else -> return current
        }
    }
}

/**
 * Whether the first statement of the declaration's block body is a `contract { ... }` call, mirroring
 * `isLegacyContractPresentPsiCheck` and `isContractDescriptionCallPsiCheck` from `ktPsiUtil.kt`.
 */
internal fun LighterAST.isLegacyContractPresent(declarationWithBody: LighterASTNode): Boolean {
    val block = firstChildOfType(declarationWithBody, KtNodeTypes.BLOCK) ?: return false
    val firstStatement = firstExpressionChild(block) ?: return false
    val unwrapped = unwrapParenthesesLabelsAndAnnotations(firstStatement)
    return isContractDescriptionCall(unwrapped)
}

private fun LighterAST.isContractDescriptionCall(expression: LighterASTNode): Boolean = when (expression.tokenType) {
    KtNodeTypes.CALL_EXPRESSION -> {
        // Mirrors `calleeExpression?.text == "contract"`: the raw text comparison, backticks included
        val callee = firstExpressionChild(expression)
        callee != null && nodeText(callee) == "contract"
    }
    KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
        val children = expressionChildren(expression)
        val receiver = children.firstOrNull()
        val selector = children.getOrNull(1)
        receiver != null && selector != null && nodeText(receiver) == "kotlin.contracts" && isContractDescriptionCall(selector)
    }
    else -> false
}

/**
 * The receiver type reference of a function, mirroring `KtNamedFunction.receiverTypeRefByTree`: the first
 * `TYPE_REFERENCE` child before a `(` or `:`.
 */
internal fun LighterAST.functionReceiverTypeReference(function: LighterASTNode): LighterASTNode? =
    receiverTypeReferenceBefore(function, stopAt = FUNCTION_RECEIVER_STOPPERS)

/**
 * The receiver type reference of a property, mirroring `KtProperty.getReceiverTypeRefByTree`: the first
 * `TYPE_REFERENCE` child before a `:`.
 */
internal fun LighterAST.propertyReceiverTypeReference(property: LighterASTNode): LighterASTNode? =
    receiverTypeReferenceBefore(property, stopAt = PROPERTY_RECEIVER_STOPPERS)

private val FUNCTION_RECEIVER_STOPPERS = TokenSet.create(KtTokens.LPAR, KtTokens.COLON)
private val PROPERTY_RECEIVER_STOPPERS = TokenSet.create(KtTokens.COLON)

private fun LighterAST.receiverTypeReferenceBefore(declaration: LighterASTNode, stopAt: TokenSet): LighterASTNode? {
    for (child in getChildren(declaration)) {
        val type = child.tokenType
        when {
            type in stopAt -> return null
            type == KtNodeTypes.TYPE_REFERENCE -> return child
        }
    }
    return null
}

/**
 * The explicit return (or property) type reference: the first `TYPE_REFERENCE` child after the first `:` token,
 * mirroring `getTypeReference` from `TypeRefHelpers.kt`.
 */
internal fun LighterAST.returnTypeReferenceAfterColon(declaration: LighterASTNode): LighterASTNode? {
    var seenColon = false
    for (child in getChildren(declaration)) {
        val type = child.tokenType
        when {
            !seenColon && type == KtTokens.COLON -> seenColon = true
            seenColon && type == KtNodeTypes.TYPE_REFERENCE -> return child
        }
    }
    return null
}

/**
 * The selector of a qualified expression: the first expression after the operation sign, mirroring
 * `KtQualifiedExpression.getSelectorExpression`.
 */
internal fun LighterAST.qualifiedSelector(qualifiedExpression: LighterASTNode): LighterASTNode? {
    var seenOperation = false
    for (child in getChildren(qualifiedExpression)) {
        val type = child.tokenType
        when {
            !seenOperation && (type == KtTokens.DOT || type == KtTokens.SAFE_ACCESS) -> seenOperation = true
            seenOperation && isExpression(child) -> return child
        }
    }
    return null
}

/**
 * The simple name indexed as a supertype name for a `USER_TYPE` node, mirroring `KtUserType.getReferencedName`:
 * the referenced name of the type's own (rightmost) reference expression.
 */
internal fun LighterAST.userTypeReferencedName(userType: LighterASTNode): String? {
    firstChildOfType(userType, KtNodeTypes.REFERENCE_EXPRESSION)?.let {
        return referencedName(it, NAME_REFERENCE_NAME_ELEMENT_TYPES)
    }

    return firstChildOfType(userType, KtNodeTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION)
        ?.let { enumSuperclassReferencedName(it) }
}

/**
 * Mirrors `KtEnumEntrySuperclassReferenceExpression.getReferencedName`: the referenced element is the enum class
 * containing the enum entry this expression belongs to, and the name is its safe name.
 */
internal fun LighterAST.enumSuperclassReferencedName(enumSuperclassReference: LighterASTNode): String {
    var enumEntry: LighterASTNode? = getParent(enumSuperclassReference)
    while (enumEntry != null && enumEntry.tokenType != KtNodeTypes.ENUM_ENTRY) {
        enumEntry = getParent(enumEntry)
    }

    val enumClass = enumEntry
        ?.let { getParent(it) }
        ?.let { getParent(it) }
        ?.takeIf { it.tokenType == KtNodeTypes.CLASS }

    val name = enumClass?.let { declarationName(it) } ?: SpecialNames.NO_NAME_PROVIDED.asString()
    return KtPsiUtil.unquoteIdentifierOrFieldReference(name)
}

/**
 * The unqualified names indexed as superclass names of a class or object, mirroring
 * `KtClassOrObject.getSuperNames` from `ktPsiUtil.kt`: the referenced name of each supertype entry's user type,
 * plus the original imported name when that name is an import alias.
 */
internal fun LighterAST.superNames(classOrObject: LighterASTNode): List<String> {
    val superTypeList = firstChildOfType(classOrObject, KtNodeTypes.SUPER_TYPE_LIST) ?: return emptyList()

    val result = ArrayList<String>()
    for (entry in getChildren(superTypeList)) {
        if (entry.tokenType !in KtTokenSets.SUPER_TYPE_LIST_ENTRIES) continue

        // Mirrors `KtSuperTypeListEntry.getTypeReference`, where a call entry takes it from its constructor callee
        val typeReference = when (entry.tokenType) {
            KtNodeTypes.SUPER_TYPE_CALL_ENTRY ->
                firstChildOfType(entry, KtNodeTypes.CONSTRUCTOR_CALLEE)
                    ?.let { firstChildOfType(it, KtNodeTypes.TYPE_REFERENCE) }
            else -> firstChildOfType(entry, KtNodeTypes.TYPE_REFERENCE)
        } ?: continue

        val typeElement = firstChildOfType(typeReference, KtTokenSets.TYPE_ELEMENT_TYPES)
        val userType = typeElement?.takeIf { it.tokenType == KtNodeTypes.USER_TYPE } ?: continue
        val referencedName = userTypeReferencedName(userType) ?: continue

        result += referencedName
        importedSimpleNameByImportAlias(referencedName)?.let { result += it }
    }

    return result
}

/**
 * The original simple name imported under [aliasName], or `null` if the name is not an import alias in this file.
 * Mirrors `getImportedSimpleNameByImportAlias` and `KtCommonFile.findImportByAlias`.
 */
internal fun LighterAST.importedSimpleNameByImportAlias(aliasName: String): String? {
    val importList = firstChildOfType(root, KtNodeTypes.IMPORT_LIST) ?: return null
    val directive = getChildren(importList).firstOrNull { directive ->
        directive.tokenType == KtNodeTypes.IMPORT_DIRECTIVE && importAliasName(directive) == aliasName
    } ?: return null

    var reference = firstChildOfType(directive, KtTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS)
    while (reference != null && reference.tokenType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION) {
        reference = qualifiedSelector(reference)
    }

    if (reference != null && reference.tokenType == KtNodeTypes.REFERENCE_EXPRESSION) {
        return referencedName(reference, NAME_REFERENCE_NAME_ELEMENT_TYPES)
    }

    return null
}

/** Mirrors `KtImportDirective.getAliasName`, which takes the alias name identifier text without unquoting. */
private fun LighterAST.importAliasName(importDirective: LighterASTNode): String? =
    firstChildOfType(importDirective, KtNodeTypes.IMPORT_ALIAS)
        ?.let { nameIdentifier(it) }
        ?.let { nodeText(it) }

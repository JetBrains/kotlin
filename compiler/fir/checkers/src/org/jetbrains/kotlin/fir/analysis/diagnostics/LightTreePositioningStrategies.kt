/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.MODALITY_MODIFIERS
import org.jetbrains.kotlin.lexer.KtTokens.VISIBILITY_MODIFIERS
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

object LightTreePositioningStrategies {
    val DEFAULT = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.OBJECT_LITERAL -> {
                    val objectKeyword = tree.findDescendantByType(node, KtTokens.OBJECT_KEYWORD)!!
                    return markElement(objectKeyword, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.OBJECT_DECLARATION -> {
                    val objectKeyword = tree.objectKeyword(node)!!
                    return markRange(
                        from = objectKeyword,
                        to = tree.nameIdentifier(node) ?: objectKeyword,
                        startOffset, endOffset, tree, node
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    return SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(node, startOffset, endOffset, tree)
                }
            }
            return super.mark(node, startOffset, endOffset, tree)
        }
    }

    val SUPERTYPES_LIST = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val target = tree.supertypesList(node) ?: node
            return markElement(target, startOffset, endOffset, tree, node)
        }
    }

    val VAL_OR_VAR_NODE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val target = tree.valOrVarKeyword(node) ?: node
            return markElement(target, startOffset, endOffset, tree, node)
        }
    }

    val COMPANION_OBJECT: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val target = tree.companionKeyword(node) ?: node
            return markElement(target, startOffset, endOffset, tree, node)
        }
    }

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.SECONDARY_CONSTRUCTOR -> {
                    val valueParameterList = tree.valueParameterList(node)
                        ?: return markElement(node, startOffset, endOffset, tree)
                    return markRange(
                        tree.constructorKeyword(node)!!,
                        tree.lastChild(valueParameterList) ?: valueParameterList,
                        startOffset, endOffset, tree, node
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    val delegationReference = tree.findChildByType(node, KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
                    if (delegationReference != null && tree.firstChild(delegationReference) == null) {
                        val constructor = tree.findParentOfType(node, KtNodeTypes.SECONDARY_CONSTRUCTOR)!!
                        val valueParameterList = tree.valueParameterList(constructor)
                            ?: return markElement(constructor, startOffset, endOffset, tree, node)
                        return markRange(
                            tree.constructorKeyword(constructor)!!,
                            tree.lastChild(valueParameterList) ?: valueParameterList,
                            startOffset, endOffset, tree, node
                        )
                    }
                    return markElement(delegationReference ?: node, startOffset, endOffset, tree, node)
                }
                else -> error("unexpected element $node")
            }
        }
    }

    val DECLARATION_RETURN_TYPE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> = markElement(getElementToMark(node, tree), startOffset, endOffset, tree, node)

        override fun isValid(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean =
            super.isValid(getElementToMark(node, tree), tree)

        private fun getElementToMark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode {
            val (returnTypeRef, nameIdentifierOrPlaceHolder) = when {
                node.tokenType == KtNodeTypes.PROPERTY_ACCESSOR ->
                    tree.typeReference(node) to tree.accessorNamePlaceholder(node)
                node.isDeclaration ->
                    tree.typeReference(node) to tree.nameIdentifier(node)
                else ->
                    null to null
            }
            return returnTypeRef ?: (nameIdentifierOrPlaceHolder ?: node)
        }
    }

    val DECLARATION_NAME: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val nameIdentifier = tree.nameIdentifier(node)
            if (nameIdentifier != null) {
                if (node.tokenType == KtNodeTypes.CLASS || node.tokenType == KtNodeTypes.OBJECT_DECLARATION) {
                    val startElement =
                        tree.modifierList(node)?.let { modifierList -> tree.findChildByType(modifierList, KtTokens.ENUM_KEYWORD) }
                            ?: tree.findChildByType(node, TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD))
                            ?: node

                    return markRange(startElement, nameIdentifier, startOffset, endOffset, tree, node)
                }
                return markElement(nameIdentifier, startOffset, endOffset, tree, node)
            }
            if (node.tokenType == KtNodeTypes.FUN) {
                return DECLARATION_SIGNATURE.mark(node, startOffset, endOffset, tree)
            }
            return DEFAULT.mark(node, startOffset, endOffset, tree)
        }

        override fun isValid(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
            //in FE 1.0 this is part of DeclarationHeader abstract strategy
            if (node.tokenType != KtNodeTypes.OBJECT_DECLARATION
                && node.tokenType != KtNodeTypes.FUN
                && node.tokenType != KtNodeTypes.SECONDARY_CONSTRUCTOR
                && node.tokenType != KtNodeTypes.OBJECT_LITERAL
            ) {
                if (tree.nameIdentifier(node) == null) {
                    return false
                }
            }
            return super.isValid(node, tree)
        }
    }

    val DECLARATION_SIGNATURE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.PRIMARY_CONSTRUCTOR, KtNodeTypes.SECONDARY_CONSTRUCTOR -> {
                    val begin = tree.constructorKeyword(node) ?: tree.valueParameterList(node)
                    ?: return markElement(node, startOffset, endOffset, tree)
                    val end = tree.valueParameterList(node) ?: tree.constructorKeyword(node)
                    ?: return markElement(node, startOffset, endOffset, tree)
                    return markRange(begin, end, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.FUN, KtNodeTypes.FUNCTION_LITERAL -> {
                    val endOfSignatureElement =
                        tree.typeReference(node)
                            ?: tree.valueParameterList(node)
                            ?: tree.nameIdentifier(node)
                            ?: node
                    val startElement = if (node.tokenType == KtNodeTypes.FUNCTION_LITERAL) {
                        tree.receiverTypeReference(node)
                            ?: tree.valueParameterList(node)
                            ?: node
                    } else node
                    return markRange(startElement, endOfSignatureElement, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.PROPERTY -> {
                    val endOfSignatureElement = tree.typeReference(node) ?: tree.nameIdentifier(node) ?: node
                    return markRange(node, endOfSignatureElement, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.PROPERTY_ACCESSOR -> {
                    val endOfSignatureElement =
                        tree.typeReference(node)
                            ?: tree.rightParenthesis(node)
                            ?: tree.accessorNamePlaceholder(node)

                    return markRange(node, endOfSignatureElement, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.CLASS -> {
                    val nameAsDeclaration = tree.nameIdentifier(node)
                        ?: return markElement(node, startOffset, endOffset, tree)
                    val primaryConstructorParameterList = tree.primaryConstructor(node)?.let { constructor ->
                        tree.valueParameterList(constructor)
                    } ?: return markElement(nameAsDeclaration, startOffset, endOffset, tree, node)
                    return markRange(nameAsDeclaration, primaryConstructorParameterList, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.OBJECT_DECLARATION -> {
                    return DECLARATION_NAME.mark(node, startOffset, endOffset, tree)
                }
                KtNodeTypes.CLASS_INITIALIZER -> {
                    return markElement(tree.initKeyword(node)!!, startOffset, endOffset, tree, node)
                }
            }
            return super.mark(node, startOffset, endOffset, tree)
        }
    }

    val DECLARATION_SIGNATURE_OR_DEFAULT: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> =
            if (node.isDeclaration) {
                DECLARATION_SIGNATURE.mark(node, startOffset, endOffset, tree)
            } else {
                DEFAULT.mark(node, startOffset, endOffset, tree)
            }
    }

    val LAST_CHILD: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val value = tree.lastChild(node) ?: node
            return markElement(value, startOffset, endOffset, tree, node)
        }
    }

    private val LighterASTNode.isDeclaration: Boolean
        get() =
            when (tokenType) {
                KtNodeTypes.PRIMARY_CONSTRUCTOR, KtNodeTypes.SECONDARY_CONSTRUCTOR,
                KtNodeTypes.FUN, KtNodeTypes.FUNCTION_LITERAL,
                KtNodeTypes.PROPERTY,
                KtNodeTypes.PROPERTY_ACCESSOR,
                KtNodeTypes.CLASS,
                KtNodeTypes.OBJECT_DECLARATION,
                KtNodeTypes.CLASS_INITIALIZER ->
                    true
                else ->
                    false
            }

    private class ModifierSetBasedLightTreePositioningStrategy(private val modifierSet: TokenSet) : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val modifierList = tree.modifierList(node)
            if (modifierList != null) {
                tree.findChildByType(modifierList, modifierSet)?.let {
                    return markElement(it, startOffset, endOffset, tree, node)
                }
            }
            tree.nameIdentifier(node)?.let {
                return markElement(it, startOffset, endOffset, tree, node)
            }
            return when (node.tokenType) {
                KtNodeTypes.OBJECT_DECLARATION -> {
                    markElement(tree.objectKeyword(node)!!, startOffset, endOffset, tree, node)
                }
                KtNodeTypes.PROPERTY_ACCESSOR -> {
                    markElement(tree.accessorNamePlaceholder(node), startOffset, endOffset, tree, node)
                }
                else -> markElement(node, startOffset, endOffset, tree)
            }
        }
    }

    val VISIBILITY_MODIFIER: LightTreePositioningStrategy = ModifierSetBasedLightTreePositioningStrategy(VISIBILITY_MODIFIERS)

    val MODALITY_MODIFIER: LightTreePositioningStrategy = ModifierSetBasedLightTreePositioningStrategy(MODALITY_MODIFIERS)

    val ABSTRACT_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.ABSTRACT_KEYWORD))

    val OPEN_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.OPEN_KEYWORD))

    val OVERRIDE_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.OVERRIDE_KEYWORD))

    val PRIVATE_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.PRIVATE_KEYWORD))

    val LATEINIT_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.LATEINIT_KEYWORD))

    val VARIANCE_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.OUT_KEYWORD))

    val CONST_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.CONST_KEYWORD))

    val INLINE_OR_VALUE_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.INLINE_KEYWORD, KtTokens.VALUE_KEYWORD))

    val INNER_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.INNER_KEYWORD))

    val OPERATOR: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.operationReference(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val PARAMETER_DEFAULT_VALUE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val defaultValueElement = tree.defaultValue(node) ?: node
            return markElement(defaultValueElement, startOffset, endOffset, tree, node)
        }
    }

    val PARAMETER_VARARG_MODIFIER: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val modifier = tree.modifierList(node)?.let { modifierList -> tree.findChildByType(modifierList, KtTokens.VARARG_KEYWORD) }
            return markElement(modifier ?: node, startOffset, endOffset, tree, node)
        }
    }

    val NAME_OF_NAMED_ARGUMENT: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return tree.findChildByType(node, KtNodeTypes.VALUE_ARGUMENT_NAME)?.let { valueArgumentName ->
                markElement(valueArgumentName, startOffset, endOffset, tree, node)
            } ?: markElement(node, startOffset, endOffset, tree, node)
        }
    }

    val VALUE_ARGUMENTS: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            //PSI counterpart simply search for first RPAR descendant, but this one is more correct
            return tree.findDescendantByType(node, KtNodeTypes.VALUE_ARGUMENT_LIST)?.let { valueArgumentList ->
                tree.findLastChildByType(valueArgumentList, KtTokens.RPAR)?.let { rpar ->
                    markElement(rpar, startOffset, endOffset, tree, node)
                }
            } ?: markElement(node, startOffset, endOffset, tree, node)
        }
    }

    val DOT_BY_QUALIFIED: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            if (node.tokenType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION) {
                return markElement(tree.dotOperator(node) ?: node, startOffset, endOffset, tree, node)
            }
            // Fallback to mark the callee reference.
            return REFERENCE_BY_QUALIFIED.mark(node, startOffset, endOffset, tree)
        }
    }

    val SELECTOR_BY_QUALIFIED: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            if (node.tokenType != KtNodeTypes.DOT_QUALIFIED_EXPRESSION && node.tokenType != KtNodeTypes.SAFE_ACCESS_EXPRESSION) {
                return super.mark(node, startOffset, endOffset, tree)
            }
            val selector = tree.selector(node)
            if (selector != null) {
                return markElement(selector, startOffset, endOffset, tree, node)
            }
            return super.mark(node, startOffset, endOffset, tree)
        }
    }

    val REFERENCE_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(false)
    val REFERENCED_NAME_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(true)

    /**
     * @param locateReferencedName see doc on [referenceExpression]
     */
    class FindReferencePositioningStrategy(val locateReferencedName: Boolean) : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            if (node.tokenType == KtNodeTypes.CALL_EXPRESSION || node.tokenType == KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL) {
                return markElement(tree.referenceExpression(node, locateReferencedName) ?: node, startOffset, endOffset, tree, node)
            }
            if (node.tokenType == KtNodeTypes.PROPERTY_DELEGATE) {
                return markElement(tree.findReferenceExpressionDeep(node) ?: node, startOffset, endOffset, tree, node)
            }
            if (node.tokenType in nodeTypesWithOperation) {
                return markElement(tree.operationReference(node) ?: node, startOffset, endOffset, tree, node)
            }
            if (node.tokenType != KtNodeTypes.DOT_QUALIFIED_EXPRESSION &&
                node.tokenType != KtNodeTypes.SAFE_ACCESS_EXPRESSION &&
                node.tokenType != KtNodeTypes.CALLABLE_REFERENCE_EXPRESSION
            ) {
                return super.mark(node, startOffset, endOffset, tree)
            }
            val selector = tree.selector(node)
            if (selector != null) {
                when (selector.tokenType) {
                    KtNodeTypes.REFERENCE_EXPRESSION ->
                        return markElement(selector, startOffset, endOffset, tree, node)
                    KtNodeTypes.CALL_EXPRESSION, KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL,KtNodeTypes.SUPER_TYPE_CALL_ENTRY ->
                        return markElement(
                            tree.referenceExpression(selector, locateReferencedName) ?: selector,
                            startOffset,
                            endOffset,
                            tree,
                            node
                        )
                }
            }
            return super.mark(node, startOffset, endOffset, tree)
        }
    }

    private val nodeTypesWithOperation = setOf(
        KtNodeTypes.IS_EXPRESSION,
        KtNodeTypes.BINARY_WITH_TYPE,
        KtNodeTypes.BINARY_EXPRESSION,
        KtNodeTypes.POSTFIX_EXPRESSION,
        KtNodeTypes.PREFIX_EXPRESSION,
        KtNodeTypes.BINARY_EXPRESSION,
        KtNodeTypes.WHEN_CONDITION_IN_RANGE
    )

    val WHEN_EXPRESSION = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.whenKeyword(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val IF_EXPRESSION = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.ifKeyword(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val ELSE_ENTRY = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.elseKeyword(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val ARRAY_ACCESS = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.findChildByType(node, KtNodeTypes.INDICES)!!, startOffset, endOffset, tree, node)
        }
    }

    val SAFE_ACCESS = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            return markElement(tree.safeAccess(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val RETURN_WITH_LABEL = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val labeledExpression = tree.findChildByType(node, KtNodeTypes.LABEL_QUALIFIER)
            if (labeledExpression != null) {
                return markRange(node, labeledExpression, startOffset, endOffset, tree, node)
            }
            return markElement(tree.returnKeyword(node) ?: node, startOffset, endOffset, tree, node)
        }
    }

    val WHOLE_ELEMENT = object : LightTreePositioningStrategy() { }

    val LONG_LITERAL_SUFFIX = object : LightTreePositioningStrategy() {
    }

    val REIFIED_MODIFIER: LightTreePositioningStrategy =
        ModifierSetBasedLightTreePositioningStrategy(TokenSet.create(KtTokens.REIFIED_KEYWORD))
}

fun FirSourceElement.hasValOrVar(): Boolean =
    treeStructure.valOrVarKeyword(lighterASTNode) != null

fun FirSourceElement.hasVar(): Boolean =
    treeStructure.findChildByType(lighterASTNode, KtTokens.VAR_KEYWORD) != null

fun FirSourceElement.hasPrimaryConstructor(): Boolean =
    treeStructure.primaryConstructor(lighterASTNode) != null

private fun FlyweightCapableTreeStructure<LighterASTNode>.companionKeyword(node: LighterASTNode): LighterASTNode? =
    modifierList(node)?.let { findChildByType(it, KtTokens.COMPANION_KEYWORD) }

private fun FlyweightCapableTreeStructure<LighterASTNode>.constructorKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.CONSTRUCTOR_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.dotOperator(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.DOT)

private fun FlyweightCapableTreeStructure<LighterASTNode>.safeAccess(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.SAFE_ACCESS)

private fun FlyweightCapableTreeStructure<LighterASTNode>.initKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.INIT_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.whenKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.WHEN_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.ifKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IF_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.elseKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.ELSE_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.returnKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.RETURN_KEYWORD)

internal fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IDENTIFIER)

private fun FlyweightCapableTreeStructure<LighterASTNode>.operationReference(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.OPERATION_REFERENCE)

private val EXPRESSION_NODE_TYPES = setOf(
    KtNodeTypes.REFERENCE_EXPRESSION,
    KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE,
    KtNodeTypes.SUPER_EXPRESSION,
    KtNodeTypes.ARRAY_ACCESS_EXPRESSION,
    KtNodeTypes.CALL_EXPRESSION,
    KtNodeTypes.LABELED_EXPRESSION,
    KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
    KtNodeTypes.FUN,
    KtNodeTypes.IF,
    KtNodeTypes.STRING_TEMPLATE,
    KtNodeTypes.LAMBDA_EXPRESSION,
)

/**
 * @param locateReferencedName whether to remove any nested parentheses while locating the reference element. This is useful for diagnostics
 * on super and unresolved references. For example, with the following, only the part inside the parentheses should be highlighted.
 *
 * ```
 * fun foo() {
 *   (super)()
 *    ^^^^^
 *   (random123)()
 *    ^^^^^^^^^
 * }
 * ```
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.referenceExpression(
    node: LighterASTNode,
    locateReferencedName: Boolean
): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    var result = childrenRef.get()?.firstOrNull {
        it?.tokenType in EXPRESSION_NODE_TYPES ||
                it?.tokenType is KtConstantExpressionElementType ||
                it?.tokenType == KtNodeTypes.PARENTHESIZED
    }
    while (locateReferencedName && result != null && result.tokenType == KtNodeTypes.PARENTHESIZED) {
        result = referenceExpression(result, locateReferencedName = true)
    }
    return result
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findReferenceExpressionDeep(node: LighterASTNode): LighterASTNode? =
    findDescendantByTypes(node, EXPRESSION_NODE_TYPES)

private fun FlyweightCapableTreeStructure<LighterASTNode>.rightParenthesis(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.RPAR)

private fun FlyweightCapableTreeStructure<LighterASTNode>.objectKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.OBJECT_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.valOrVarKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, VAL_VAR_TOKEN_SET)

internal fun FlyweightCapableTreeStructure<LighterASTNode>.visibilityModifier(declaration: LighterASTNode): LighterASTNode? =
    modifierList(declaration)?.let { findChildByType(it, VISIBILITY_MODIFIERS) }

internal fun FlyweightCapableTreeStructure<LighterASTNode>.modalityModifier(declaration: LighterASTNode): LighterASTNode? =
    modifierList(declaration)?.let { findChildByType(it, MODALITY_MODIFIERS) }

internal fun FlyweightCapableTreeStructure<LighterASTNode>.overrideModifier(declaration: LighterASTNode): LighterASTNode? =
    modifierList(declaration)?.let { findChildByType(it, KtTokens.OVERRIDE_KEYWORD) }

internal fun FlyweightCapableTreeStructure<LighterASTNode>.typeParametersList(declaration: LighterASTNode): LighterASTNode? =
    findChildByType(declaration, KtNodeTypes.TYPE_PARAMETER_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.supertypesList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.SUPER_TYPE_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.accessorNamePlaceholder(node: LighterASTNode): LighterASTNode =
    findChildByType(node, KtTokens.GET_KEYWORD) ?: findChildByType(node, KtTokens.SET_KEYWORD)!!

private fun FlyweightCapableTreeStructure<LighterASTNode>.modifierList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.MODIFIER_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.primaryConstructor(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.PRIMARY_CONSTRUCTOR)

private fun FlyweightCapableTreeStructure<LighterASTNode>.valueParameterList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.VALUE_PARAMETER_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.typeReference(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filterNotNull()?.dropWhile { it.tokenType != KtTokens.COLON }?.firstOrNull {
        it.tokenType == KtNodeTypes.TYPE_REFERENCE
    }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.receiverTypeReference(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filterNotNull()?.firstOrNull {
        if (it.tokenType == KtTokens.COLON || it.tokenType == KtTokens.LPAR) return null
        it.tokenType == KtNodeTypes.TYPE_REFERENCE
    }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.defaultValue(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    // p : T = v
    val children = childrenRef.get()?.reversed() ?: return null
    for (child in children) {
        if (child == null || child.tokenType == KtTokens.WHITE_SPACE) continue
        if (child.tokenType == KtNodeTypes.TYPE_REFERENCE || child.tokenType == KtTokens.COLON) return null
        return child
    }
    return null
}

fun FlyweightCapableTreeStructure<LighterASTNode>.selector(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    val children = childrenRef.get() ?: return null
    var dotOrDoubleColonFound = false
    for (child in children) {
        if (child == null) continue
        if (child.tokenType == KtTokens.DOT || child.tokenType == KtTokens.COLONCOLON) {
            dotOrDoubleColonFound = true
            continue
        }
        if (dotOrDoubleColonFound && (child.tokenType == KtNodeTypes.CALL_EXPRESSION || child.tokenType == KtNodeTypes.REFERENCE_EXPRESSION)) {
            return child
        }
    }
    return null

}

fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType == type }
}

fun FlyweightCapableTreeStructure<LighterASTNode>.findLastChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.lastOrNull { it?.tokenType == type }
}

fun FlyweightCapableTreeStructure<LighterASTNode>.findDescendantByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType == type } ?: childrenRef.get()
        ?.firstNotNullResult { child -> child?.let { findDescendantByType(it, type) } }
}

fun FlyweightCapableTreeStructure<LighterASTNode>.findDescendantByTypes(node: LighterASTNode, types: Set<IElementType>): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { types.contains(it?.tokenType) } ?: childrenRef.get()
        ?.firstNotNullResult { child -> child?.let { findDescendantByTypes(it, types) } }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: TokenSet): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType in type }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findParentOfType(
    node: LighterASTNode,
    type: IElementType,
    strict: Boolean = true
): LighterASTNode? {
    if (!strict && node.tokenType == type) return node
    var parent = getParent(node)
    while (parent != null) {
        if (parent.tokenType == type) return parent
        parent = getParent(parent)
    }
    return null
}

internal fun FlyweightCapableTreeStructure<LighterASTNode>.getAncestors(node: LighterASTNode): Sequence<LighterASTNode> =
    generateSequence(getParent(node)) { getParent(it) }

private fun FlyweightCapableTreeStructure<LighterASTNode>.firstChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull()
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.lastChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get().lastOrNull()
}

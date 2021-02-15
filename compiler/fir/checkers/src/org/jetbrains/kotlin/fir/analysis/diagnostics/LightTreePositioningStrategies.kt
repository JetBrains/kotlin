/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.MODALITY_MODIFIERS
import org.jetbrains.kotlin.lexer.KtTokens.VISIBILITY_MODIFIERS
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET

object LightTreePositioningStrategies {
    val DEFAULT = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            when (node.tokenType) {
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
        ): List<TextRange> {
            val (returnTypeRef, nameIdentifierOrPlaceHolder) = when {
                node.tokenType == KtNodeTypes.PROPERTY_ACCESSOR ->
                    tree.typeReference(node) to tree.accessorNamePlaceholder(node)
                node.isDeclaration ->
                    tree.typeReference(node) to tree.nameIdentifier(node)
                else ->
                    null to null
            }
            if (returnTypeRef != null) {
                return markElement(returnTypeRef, startOffset, endOffset, tree, node)
            }
            if (nameIdentifierOrPlaceHolder != null) {
                return markElement(nameIdentifierOrPlaceHolder, startOffset, endOffset, tree, node)
            }
            return DEFAULT.mark(node, startOffset, endOffset, tree)
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
                return markElement(node, startOffset, endOffset, tree)
            }
            if (node.tokenType == KtNodeTypes.FUN) {
                return DECLARATION_SIGNATURE.mark(node, startOffset, endOffset, tree)
            }
            return DEFAULT.mark(node, startOffset, endOffset, tree)
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

    val DOT_BY_SELECTOR: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            if (node.tokenType != KtNodeTypes.REFERENCE_EXPRESSION && node.tokenType != KtNodeTypes.CALL_EXPRESSION) {
                // TODO: normally CALL_EXPRESSION should not be here. In PSI we have REFERENCE_EXPRESSION even for x.bar() case
                // Remove CALL_EXPRESSION from here and repeat code below twice (see PSI counterpart) when fixed
                return super.mark(node, startOffset, endOffset, tree)
            }
            val parentNode = tree.getParent(node) ?: return super.mark(node, startOffset, endOffset, tree)
            if (parentNode.tokenType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION) {
                return markElement(tree.dotOperator(parentNode) ?: node, startOffset, endOffset, tree, node)
            }
            return super.mark(node, startOffset, endOffset, tree)
        }
    }

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
}

fun FirSourceElement.hasValOrVar(): Boolean =
    treeStructure.valOrVarKeyword(lighterASTNode) != null

fun FirSourceElement.hasVar(): Boolean =
    treeStructure.findChildByType(lighterASTNode, KtTokens.VAR_KEYWORD) != null

fun FirSourceElement.hasPrimaryConstructor(): Boolean =
    treeStructure.primaryConstructor(lighterASTNode) != null

private fun FlyweightCapableTreeStructure<LighterASTNode>.constructorKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.CONSTRUCTOR_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.dotOperator(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.DOT)

private fun FlyweightCapableTreeStructure<LighterASTNode>.initKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.INIT_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.whenKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.WHEN_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.ifKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IF_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IDENTIFIER)

private fun FlyweightCapableTreeStructure<LighterASTNode>.operationReference(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.OPERATION_REFERENCE)

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

fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType == type }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: TokenSet): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType in type }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findParentOfType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    var parent = getParent(node)
    while (parent != null) {
        if (parent.tokenType == type) return parent
        parent = getParent(parent)
    }
    return null
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.firstChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull()
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.lastChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.lastOrNull()
}

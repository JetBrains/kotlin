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
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET

object LightTreePositioningStrategies {
    val DEFAULT = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.OBJECT_DECLARATION -> {
                    val objectKeyword = tree.objectKeyword(node)!!
                    return markRange(
                        from = objectKeyword,
                        to = tree.nameIdentifier(node) ?: objectKeyword,
                        tree
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    return SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(node, tree)
                }
            }
            return super.mark(node, tree)
        }
    }

    val VAL_OR_VAR_NODE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            val target = tree.valOrVarKeyword(node) ?: node
            return markElement(target, tree)
        }
    }

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.SECONDARY_CONSTRUCTOR -> {
                    val valueParameterList = tree.valueParameterList(node) ?: return markElement(node, tree)
                    return markRange(
                        tree.constructorKeyword(node)!!,
                        tree.lastChild(valueParameterList)!!, tree
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    val delegationReference = tree.findChildByType(node, KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
                    if (delegationReference != null && tree.firstChild(delegationReference) == null) {
                        val constructor = tree.findParentOfType(node, KtNodeTypes.SECONDARY_CONSTRUCTOR)!!
                        val valueParameterList = tree.valueParameterList(constructor)
                            ?: return markElement(constructor, tree)
                        return markRange(
                            tree.constructorKeyword(constructor)!!,
                            tree.lastChild(valueParameterList)!!, tree
                        )
                    }
                    return markElement(delegationReference ?: node, tree)
                }
                else -> error("unexpected element $node")
            }
        }
    }

    val DECLARATION_NAME: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            val nameIdentifier = tree.nameIdentifier(node)
            if (nameIdentifier != null) {
                if (node.tokenType == KtNodeTypes.CLASS || node.tokenType == KtNodeTypes.OBJECT_DECLARATION) {
                    val startElement =
                        tree.modifierList(node)?.let { modifierList -> tree.findChildByType(modifierList, KtTokens.ENUM_KEYWORD) }
                            ?: tree.findChildByType(node, TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD))
                            ?: node

                    return markRange(startElement, nameIdentifier, tree)
                }
                return markElement(nameIdentifier, tree)
            }
            if (node.tokenType == KtNodeTypes.FUN) {
                return DECLARATION_SIGNATURE.mark(node, tree)
            }
            return DEFAULT.mark(node, tree)
        }
    }

    val DECLARATION_SIGNATURE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.PRIMARY_CONSTRUCTOR, KtNodeTypes.SECONDARY_CONSTRUCTOR -> {
                    val begin = tree.constructorKeyword(node) ?: tree.valueParameterList(node) ?: return markElement(node, tree)
                    val end = tree.valueParameterList(node) ?: tree.constructorKeyword(node) ?: return markElement(node, tree)
                    return markRange(begin, end, tree)
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
                    return markRange(startElement, endOfSignatureElement, tree)
                }
                KtNodeTypes.PROPERTY -> {
                    val endOfSignatureElement = tree.typeReference(node) ?: tree.nameIdentifier(node) ?: node
                    return markRange(node, endOfSignatureElement, tree)
                }
                KtNodeTypes.PROPERTY_ACCESSOR -> {
                    val endOfSignatureElement =
                        tree.typeReference(node)
                            ?: tree.rightParenthesis(node)
                            ?: tree.accessorNamePlaceholder(node)

                    return markRange(node, endOfSignatureElement, tree)
                }
                KtNodeTypes.CLASS -> {
                    val nameAsDeclaration = tree.nameIdentifier(node) ?: return markElement(node, tree)
                    val primaryConstructorParameterList = tree.primaryConstructor(node)?.let { constructor ->
                        tree.valueParameterList(constructor)
                    } ?: return markElement(nameAsDeclaration, tree)
                    return markRange(nameAsDeclaration, primaryConstructorParameterList, tree)
                }
                KtNodeTypes.OBJECT_DECLARATION -> {
                    return DECLARATION_NAME.mark(node, tree)
                }
                KtNodeTypes.CLASS_INITIALIZER -> {
                    return markElement(tree.initKeyword(node)!!, tree)
                }
            }
            return super.mark(node, tree)
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

private fun FlyweightCapableTreeStructure<LighterASTNode>.initKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.INIT_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IDENTIFIER)

private fun FlyweightCapableTreeStructure<LighterASTNode>.rightParenthesis(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.RPAR)

private fun FlyweightCapableTreeStructure<LighterASTNode>.objectKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.OBJECT_KEYWORD)

private fun FlyweightCapableTreeStructure<LighterASTNode>.valOrVarKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, VAL_VAR_TOKEN_SET)

private fun FlyweightCapableTreeStructure<LighterASTNode>.accessorNamePlaceholder(node: LighterASTNode): LighterASTNode =
    findChildByType(node, KtTokens.GET_KEYWORD) ?: findChildByType(node, KtTokens.SET_KEYWORD)!!

private fun FlyweightCapableTreeStructure<LighterASTNode>.modifierList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.MODIFIER_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.primaryConstructor(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.PRIMARY_CONSTRUCTOR)

private fun FlyweightCapableTreeStructure<LighterASTNode>.valueParameterList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.VALUE_PARAMETER_LIST)

private fun FlyweightCapableTreeStructure<LighterASTNode>.typeReference(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.dropWhile { it.tokenType != KtTokens.COLON }?.firstOrNull { it.tokenType == KtNodeTypes.TYPE_REFERENCE }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.receiverTypeReference(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull {
        if (it.tokenType == KtTokens.COLON || it.tokenType == KtTokens.LPAR) return null
        it.tokenType == KtNodeTypes.TYPE_REFERENCE
    }
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

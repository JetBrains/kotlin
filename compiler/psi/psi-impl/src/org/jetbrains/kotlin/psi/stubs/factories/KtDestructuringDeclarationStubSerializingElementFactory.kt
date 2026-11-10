/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.stubs.impl.KotlinDestructuringDeclarationStubImpl
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.VAL_VAR_TOKENS
import org.jetbrains.kotlin.psi.stubs.light.initializerAfterEq

internal object KtDestructuringDeclarationStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinDestructuringDeclarationStubImpl, KtDestructuringDeclaration>(
        type = KtNodeTypes.DESTRUCTURING_DECLARATION,
    ) {

    override fun createPsi(
        stub: KotlinDestructuringDeclarationStubImpl,
    ): KtDestructuringDeclaration = KtDestructuringDeclaration(stub)

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent
        return when (parent?.elementType) {
            KtNodeTypes.FILE, KtNodeTypes.CLASS_BODY -> true
            KtNodeTypes.BLOCK -> parent.treeParent?.elementType == KtNodeTypes.SCRIPT
            else -> false
        }
    }

    override fun createStub(
        psi: KtDestructuringDeclaration,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationStubImpl = KotlinDestructuringDeclarationStubImpl(
        parent = parentStub,
        isVar = psi.isVar,
        hasInitializer = psi.hasInitializer(),
        hasSquareBrackets = psi.hasSquareBrackets(),
        isFullForm = psi.isFullForm,
    )

    override fun shouldCreateStub(tree: LighterAST, node: LighterASTNode, parentStub: StubElement<*>): Boolean {
        val parent = tree.getParent(node) ?: return false
        val parentType = parent.tokenType
        return when {
            parentType is IFileElementType || parentType == KtNodeTypes.CLASS_BODY -> true
            parentType == KtNodeTypes.BLOCK -> tree.getParent(parent)?.tokenType == KtNodeTypes.SCRIPT
            else -> false
        }
    }

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinDestructuringDeclarationStubImpl = KotlinDestructuringDeclarationStubImpl(
        parent = parentStub,
        isVar = tree.firstChildOfType(node, KtTokens.VAR_KEYWORD) != null,
        hasInitializer = tree.initializerAfterEq(node) != null,
        hasSquareBrackets = tree.firstChildOfType(node, KtTokens.LBRACKET) != null,
        isFullForm = firstEntry(tree, node)
            ?.let { tree.firstChildOfType(it, VAL_VAR_TOKENS) != null } == true,
    )


    /**
     * Mirrors `KtDestructuringDeclaration.getEntries`: entries may be wrapped in error elements for top-level
     * destructuring declarations (KT-58563).
     */
    private fun firstEntry(tree: LighterAST, node: LighterASTNode): LighterASTNode? {
        for (child in tree.getChildren(node)) {
            when (child.tokenType) {
                KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY -> return child
                TokenType.ERROR_ELEMENT -> {
                    tree.getChildren(child)
                        .firstOrNull { it.tokenType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY }
                        ?.let { return it }
                }
                else -> {}
            }
        }
        return null
    }

    override fun serialize(stub: KotlinDestructuringDeclarationStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.hasInitializer)
        dataStream.writeBoolean(stub.hasSquareBrackets)
        dataStream.writeBoolean(stub.isFullForm)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationStubImpl = KotlinDestructuringDeclarationStubImpl(
        parent = parentStub,
        isVar = dataStream.readBoolean(),
        hasInitializer = dataStream.readBoolean(),
        hasSquareBrackets = dataStream.readBoolean(),
        isFullForm = dataStream.readBoolean(),
    )
}

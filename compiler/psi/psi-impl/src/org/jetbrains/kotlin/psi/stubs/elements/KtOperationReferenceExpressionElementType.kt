/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.stubs.KotlinOperationReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtOperationReferenceExpressionElementType.deserializeOperationToken
import org.jetbrains.kotlin.psi.stubs.elements.KtOperationReferenceExpressionElementType.serializeOperationToken
import org.jetbrains.kotlin.psi.stubs.impl.KotlinOperationReferenceExpressionStubImpl

@OptIn(KtImplementationDetail::class)
internal object KtOperationReferenceExpressionElementType
    : KtStubElementType<KotlinOperationReferenceExpressionStubImpl, KtOperationReferenceExpression>(
    /* debugName = */ "OPERATION_REFERENCE",
    /* psiClass = */ KtOperationReferenceExpression::class.java,
    /* stubClass = */ KotlinOperationReferenceExpressionStub::class.java,
) {
    /**
     * All tokens which can be an [operation token][KtOperationReferenceExpression.getReferencedNameElementType],
     * indexed by their persistent [KtToken.tokenId].
     *
     * The map is initialized lazily on purpose: [KtTokenSets] is initialized from [org.jetbrains.kotlin.KtNodeTypes],
     * which in turn is initialized from this element type, so eager initialization would observe uninitialized token sets.
     *
     * @see serializeOperationToken
     * @see deserializeOperationToken
     */
    private val operationTokensById: Map<Int, KtToken> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            val tokens = buildList {
                addAll(KtTokenSets.PREFIX_OPERATIONS.types)
                addAll(KtTokenSets.POSTFIX_OPERATIONS.types)
                for (precedence in BinaryOperationPrecedence.entries) {
                    addAll(precedence.tokens)
                }
            }

            for (token in tokens) {
                if (token is KtToken && token.tokenId >= 0) {
                    put(token.tokenId, token)
                }
            }
        }
    }

    override fun shouldCreateStub(node: ASTNode): Boolean {
        return StubUtils.isDeclaredInsideValueArgument(node) && super.shouldCreateStub(node)
    }

    override fun createStub(
        psi: KtOperationReferenceExpression,
        parentStub: StubElement<*>?,
    ): KotlinOperationReferenceExpressionStubImpl = KotlinOperationReferenceExpressionStubImpl(
        parent = parentStub,
        referencedNameRef = StringRef.fromString(psi.getReferencedName()),
        operationToken = psi.getReferencedNameElementType(),
    )

    override fun serialize(stub: KotlinOperationReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
        dataStream.writeVarInt(serializeOperationToken(stub.operationToken))
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinOperationReferenceExpressionStubImpl {
        val referencedNameRef = dataStream.readName()!!
        val operationToken = deserializeOperationToken(dataStream.readVarInt())

        return KotlinOperationReferenceExpressionStubImpl(
            parent = parentStub,
            referencedNameRef = referencedNameRef,
            operationToken = operationToken,
        )
    }

    /**
     * `0` is reserved for a malformed operation reference without an operation token, so [KtToken.tokenId] is shifted by one.
     */
    private fun serializeOperationToken(operationToken: IElementType): Int {
        val tokenId = (operationToken as? KtToken)?.tokenId ?: return 0
        return if (tokenId in operationTokensById) tokenId + 1 else 0
    }

    /**
     * @see serializeOperationToken
     */
    private fun deserializeOperationToken(rawTokenId: Int): IElementType {
        // The element type itself is used as a fallback, exactly as the AST-based implementation does
        return operationTokensById[rawTokenId - 1] ?: this
    }
}

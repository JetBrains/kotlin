/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.psi.stubs.impl.KotlinOperationReferenceExpressionStubImpl

internal object KtOperationReferenceExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinOperationReferenceExpressionStubImpl, KtOperationReferenceExpression>(
        type = KtNodeTypes.OPERATION_REFERENCE,
    ) {

    /**
     * All tokens which can be an [operation token][KtOperationReferenceExpression.getReferencedNameElementType],
     * indexed by their persistent [KtToken.tokenId].
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

    override fun createPsi(
        stub: KotlinOperationReferenceExpressionStubImpl,
    ): KtOperationReferenceExpression = KtOperationReferenceExpression(stub)

    override fun createStub(
        psi: KtOperationReferenceExpression,
        parentStub: StubElement<*>?,
    ): KotlinOperationReferenceExpressionStubImpl = KotlinOperationReferenceExpressionStubImpl(
        parent = parentStub,
        referencedNameRef = StringRef.fromString(psi.getReferencedName())!!,
        operationToken = psi.getReferencedNameElementType(),
    )

    override fun serialize(stub: KotlinOperationReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
        dataStream.writeVarInt(serializeOperationToken(stub.operationToken))
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinOperationReferenceExpressionStubImpl {
        val referencedNameRef = dataStream.readName()!!
        val operationToken = deserializeOperationToken(dataStream.readVarInt())

        return KotlinOperationReferenceExpressionStubImpl(
            parent = parentStub,
            referencedNameRef = referencedNameRef,
            operationToken = operationToken,
        )
    }

    /**
     * `0` is reserved for a malformed operation reference without an operation token, so [KtToken.tokenId] is
     * shifted by one.
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
        return operationTokensById[rawTokenId - 1] ?: type
    }
}

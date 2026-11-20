/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.psi.stubs.impl.KotlinOperationReferenceExpressionStubImpl

internal object KtOperationReferenceExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinOperationReferenceExpressionStubImpl, KtOperationReferenceExpression>(
        type = KtNodeTypes.OPERATION_REFERENCE,
    ) {

    /**
     * All operation signs which can be an [operation token][KtOperationReferenceExpression.getReferencedNameElementType],
     * indexed by their [value][KtSingleValueToken.value].
     *
     * The operation sign is restored from the [referenced name][KotlinOperationReferenceExpressionStubImpl.referencedName]
     * instead of persisting [KtToken.tokenId][org.jetbrains.kotlin.lexer.KtToken.tokenId], as token ids are not stable
     * and are shifted by any new token declared before the operation ones.
     *
     * @see encodeOperationToken
     * @see decodeOperationToken
     */
    private val operationSignsByValue: Map<String, KtSingleValueToken> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            val tokens = buildList {
                addAll(KtTokenSets.PREFIX_OPERATIONS.types)
                addAll(KtTokenSets.POSTFIX_OPERATIONS.types)
                for (precedence in BinaryOperationPrecedence.entries) {
                    addAll(precedence.tokens)
                }
            }

            for (token in tokens) {
                if (token is KtSingleValueToken) {
                    val existingToken = put(token.value, token)
                    require(existingToken == null || existingToken == token) {
                        "Operation signs must have unique values, but '${token.value}' is shared by $existingToken and $token"
                    }
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
        dataStream.writeVarInt(encodeOperationToken(stub.operationToken, stub.referencedName).ordinal)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinOperationReferenceExpressionStubImpl {
        val referencedNameRef = dataStream.readName()!!
        val operationToken = decodeOperationToken(dataStream.readVarInt(), referencedNameRef.string)

        return KotlinOperationReferenceExpressionStubImpl(
            parent = parentStub,
            referencedNameRef = referencedNameRef,
            operationToken = operationToken,
        )
    }

    /**
     * The kind of [operation token][KotlinOperationReferenceExpressionStubImpl.operationToken] persisted by its ordinal.
     */
    private enum class OperationTokenKind {
        /** A malformed operation reference without an operation token */
        NONE,

        /** An infix function call, the referenced name is the name of the function */
        IDENTIFIER,

        /** An operation sign, the referenced name is its [value][KtSingleValueToken.value] */
        SIGN,
    }

    private fun encodeOperationToken(operationToken: IElementType, referencedName: String): OperationTokenKind {
        if (operationToken == KtTokens.IDENTIFIER) {
            return OperationTokenKind.IDENTIFIER
        }

        if (operationToken !is KtSingleValueToken || operationSignsByValue[operationToken.value] != operationToken) {
            return OperationTokenKind.NONE
        }

        check(referencedName == operationToken.value) {
            "The referenced name '$referencedName' doesn't match the operation sign $operationToken"
        }

        return OperationTokenKind.SIGN
    }

    /**
     * @see encodeOperationToken
     */
    private fun decodeOperationToken(kindOrdinal: Int, referencedName: String): IElementType {
        val kind = OperationTokenKind.entries.getOrNull(kindOrdinal)
            ?: error("Unknown operation token kind $kindOrdinal for '$referencedName'")

        return when (kind) {
            // The element type itself is used as a fallback, exactly as the AST-based implementation does
            OperationTokenKind.NONE -> type
            OperationTokenKind.IDENTIFIER -> KtTokens.IDENTIFIER
            OperationTokenKind.SIGN -> operationSignsByValue[referencedName] ?: error("Unknown operation sign '$referencedName'")
        }
    }
}

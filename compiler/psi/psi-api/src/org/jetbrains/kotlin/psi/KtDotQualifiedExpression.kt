/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

class KtDotQualifiedExpression : KtExpressionImplStub<KotlinPlaceHolderStub<KtDotQualifiedExpression>>, KtQualifiedExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtDotQualifiedExpression>) : super(stub, KtStubBasedElementTypes.DOT_QUALIFIED_EXPRESSION)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDotQualifiedExpression(this, data)
    }

    override val receiverExpression: KtExpression
        get() = stubReceiverExpression ?: super.receiverExpression

    @KtPsiInconsistencyHandling
    override val receiverExpressionOrNull: KtExpression?
        get() = stubReceiverExpression ?: super.receiverExpressionOrNull

    private val stubReceiverExpression: KtExpression?
        get() = getChildStubValue(index = 0)

    override val selectorExpression: KtExpression?
        get() = getChildStubValue(index = 1) ?: super.selectorExpression

    private fun getChildStubValue(index: Int): KtExpression? {
        val stub = stub
        if (stub != null) {
            val childExpressionsByStub = getChildExpressionsByStub(stub)
            // If one part is missing in the stub, we cannot be sure whether we have a receiver or a selector
            if (childExpressionsByStub != null && childExpressionsByStub.size == 2) {
                return childExpressionsByStub[index]
            }
        }

        return null
    }

    private fun getChildExpressionsByStub(stub: KotlinPlaceHolderStub<KtDotQualifiedExpression>): List<KtExpression>? {
        if (stub.getParentStubOfType(KtImportDirective::class.java) == null &&
            stub.getParentStubOfType(KtPackageDirective::class.java) == null &&
            stub.getParentStubOfType(KtValueArgument::class.java) == null
        ) {
            LOG.error(
                "KtDotQualifiedExpression should only have stubs inside import, argument or package directives.\n" +
                        "Stubs were created for:\n$text\nFile text:\n${containingFile.text}"
            )
            return null
        } else {
            val expressions = stub.childrenStubs.mapNotNull { it.psi as? KtExpression }
            if (expressions.size !in 1..2) {
                LOG.logErrorWithAttachment("Invalid stub structure. DOT_QUALIFIED_EXPRESSION must have one or two children. Was: ${expressions.size}") {
                    withPsiEntry("file", containingFile)
                }
                return null
            }
            return expressions
        }
    }

    companion object {
        private val LOG = Logger.getInstance(KtDotQualifiedExpression::class.java)
    }
}

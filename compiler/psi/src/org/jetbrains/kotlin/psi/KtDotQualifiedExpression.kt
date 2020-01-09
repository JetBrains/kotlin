/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KtDotQualifiedExpression : KtExpressionImplStub<KotlinPlaceHolderStub<KtDotQualifiedExpression>>, KtQualifiedExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtDotQualifiedExpression>) : super(stub, KtStubElementTypes.DOT_QUALIFIED_EXPRESSION)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDotQualifiedExpression(this, data)
    }

    override val receiverExpression: KtExpression
        get() {
            val stub = stub
            if (stub != null) {
                val childExpressionsByStub = getChildExpressionsByStub(stub)
                if (childExpressionsByStub != null) {
                    return childExpressionsByStub[0]
                }
            }
            return super.receiverExpression
        }

    override val selectorExpression: KtExpression?
        get() {
            val stub = stub
            if (stub != null) {
                val childExpressionsByStub = getChildExpressionsByStub(stub)
                if (childExpressionsByStub != null && childExpressionsByStub.size == 2) {
                    return childExpressionsByStub[1]
                }
            }
            return super.selectorExpression
        }

    private fun getChildExpressionsByStub(stub: KotlinPlaceHolderStub<KtDotQualifiedExpression>): Array<KtExpression>? {
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
            val expressions = stub.getChildrenByType(INSIDE_DIRECTIVE_EXPRESSIONS, KtExpression.ARRAY_FACTORY)
            if (expressions.size !in 1..2) {
                LOG.error(
                    KotlinExceptionWithAttachments(
                        "Invalid stub structure. DOT_QUALIFIED_EXPRESSION must have one or two children. Was: ${expressions.size}\n"
                    ).withAttachment("file.kt", containingFile.text)
                )
                return null
            }
            return expressions
        }
    }

    companion object {
        private val LOG = Logger.getInstance(KtDotQualifiedExpression::class.java)
    }
}

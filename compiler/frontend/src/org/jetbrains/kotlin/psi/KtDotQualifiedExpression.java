/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import static org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS;

public class KtDotQualifiedExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtDotQualifiedExpression>>
        implements KtQualifiedExpression {

    private static final Logger LOG = Logger.getInstance(KtDotQualifiedExpression.class);

    public KtDotQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }

    public KtDotQualifiedExpression(@NotNull KotlinPlaceHolderStub<KtDotQualifiedExpression> stub) {
        super(stub, KtStubElementTypes.DOT_QUALIFIED_EXPRESSION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDotQualifiedExpression(this, data);
    }

    @NotNull
    @Override
    public KtExpression getReceiverExpression() {
        KotlinPlaceHolderStub<KtDotQualifiedExpression> stub = getStub();
        if (stub != null) {
            KtExpression[] childExpressionsByStub = getChildExpressionsByStub(stub);
            if (childExpressionsByStub != null) {
                return childExpressionsByStub[0];
            }
        }
        return KtQualifiedExpressionImpl.INSTANCE$.getReceiverExpression(this);
    }

    @Nullable
    @Override
    public KtExpression getSelectorExpression() {
        KotlinPlaceHolderStub<KtDotQualifiedExpression> stub = getStub();
        if (stub != null) {
            KtExpression[] childExpressionsByStub = getChildExpressionsByStub(stub);
            if (childExpressionsByStub != null && childExpressionsByStub.length == 2) {
                return childExpressionsByStub[1];
            }
        }
        return KtQualifiedExpressionImpl.INSTANCE$.getSelectorExpression(this);
    }


    @Nullable
    private KtExpression[] getChildExpressionsByStub(@NotNull KotlinPlaceHolderStub<KtDotQualifiedExpression> stub) {
        if (stub.getParentStubOfType(KtImportDirective.class) == null && stub.getParentStubOfType(KtPackageDirective.class) == null) {
            LOG.error("JetDotQualifiedExpression should only have stubs inside import or package directives.\n " +
                      "Stubs were created for:\n " + getText() +
                      "\nFile text:\n" + getContainingFile().getText());
            return null;
        }
        else {
            KtExpression[] expressions = stub.getChildrenByType(INSIDE_DIRECTIVE_EXPRESSIONS, KtExpression.ARRAY_FACTORY);
            if (expressions.length < 1 || expressions.length > 2) {
                LOG.error("Invalid stub structure. DOT_QUALIFIED_EXPRESSION must have one or two children. Was: " + expressions.length +
                          "\nFile text:\n" + getContainingFile().getText());
                return null;
            }
            return expressions;
        }
    }

    @NotNull
    @Override
    public ASTNode getOperationTokenNode() {
        return KtQualifiedExpressionImpl.INSTANCE$.getOperationTokenNode(this);
    }

    @NotNull
    @Override
    public KtToken getOperationSign() {
        return KtQualifiedExpressionImpl.INSTANCE$.getOperationSign(this);
    }
}

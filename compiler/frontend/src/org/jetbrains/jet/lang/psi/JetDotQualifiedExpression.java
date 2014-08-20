/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPlaceHolderStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetToken;

import static org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS;

public class JetDotQualifiedExpression extends JetExpressionImplStub<PsiJetPlaceHolderStub<JetDotQualifiedExpression>>
        implements JetQualifiedExpression {

    private static final Logger LOG = Logger.getInstance(JetDotQualifiedExpression.class);

    public JetDotQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }

    public JetDotQualifiedExpression(@NotNull PsiJetPlaceHolderStub<JetDotQualifiedExpression> stub) {
        super(stub, JetStubElementTypes.DOT_QUALIFIED_EXPRESSION);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDotQualifiedExpression(this, data);
    }

    @NotNull
    @Override
    public JetExpression getReceiverExpression() {
        PsiJetPlaceHolderStub<JetDotQualifiedExpression> stub = getStub();
        if (stub != null) {
            JetExpression[] childExpressionsByStub = getChildExpressionsByStub(stub);
            if (childExpressionsByStub != null) {
                return childExpressionsByStub[0];
            }
        }
        return JetQualifiedExpressionImpl.INSTANCE$.getReceiverExpression(this);
    }

    @Nullable
    @Override
    public JetExpression getSelectorExpression() {
        PsiJetPlaceHolderStub<JetDotQualifiedExpression> stub = getStub();
        if (stub != null) {
            JetExpression[] childExpressionsByStub = getChildExpressionsByStub(stub);
            if (childExpressionsByStub != null && childExpressionsByStub.length == 2) {
                return childExpressionsByStub[1];
            }
        }
        return JetQualifiedExpressionImpl.INSTANCE$.getSelectorExpression(this);
    }


    @Nullable
    private JetExpression[] getChildExpressionsByStub(@NotNull PsiJetPlaceHolderStub<JetDotQualifiedExpression> stub) {
        if (stub.getParentStubOfType(JetImportDirective.class) == null && stub.getParentStubOfType(JetPackageDirective.class) == null) {
            LOG.error("JetDotQualifiedExpression should only have stubs inside import or package directives.\n " +
                      "Stubs were created for:\n " + getText() +
                      "\nFile text:\n" + getContainingFile().getText());
            return null;
        }
        else {
            JetExpression[] expressions = stub.getChildrenByType(INSIDE_DIRECTIVE_EXPRESSIONS, JetExpression.ARRAY_FACTORY);
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
        return JetQualifiedExpressionImpl.INSTANCE$.getOperationTokenNode(this);
    }

    @NotNull
    @Override
    public JetToken getOperationSign() {
        return JetQualifiedExpressionImpl.INSTANCE$.getOperationSign(this);
    }
}

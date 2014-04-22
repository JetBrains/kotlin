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
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetSimpleNameExpression extends JetReferenceExpression {
    public static final TokenSet REFERENCE_TOKENS = TokenSet.orSet(LABELS, TokenSet.create(IDENTIFIER, FIELD_IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD));

    public JetSimpleNameExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public JetExpression getReceiverExpression() {
        PsiElement parent = getParent();
        if (parent instanceof JetQualifiedExpression && !isImportDirectiveExpression()) {
            JetExpression receiverExpression = ((JetQualifiedExpression) parent).getReceiverExpression();
            // Name expression can't be receiver for itself
            if (receiverExpression != this) {
                return receiverExpression;
            }
        }
        else if (parent instanceof JetCallExpression) {
            //This is in case `a().b()`
            JetCallExpression callExpression = (JetCallExpression) parent;
            parent = callExpression.getParent();
            if (parent instanceof JetQualifiedExpression) {
                JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) parent;
                JetExpression parentsReceiver = qualifiedExpression.getReceiverExpression();
                if (parentsReceiver != callExpression) {
                    return parentsReceiver;
                }
            }
        }
        else if (parent instanceof JetBinaryExpression && ((JetBinaryExpression) parent).getOperationReference() == this) {
            JetBinaryExpression expr = (JetBinaryExpression) parent;
            //noinspection SuspiciousMethodCalls
            return OperatorConventions.IN_OPERATIONS.contains(expr.getOperationToken())
                   ? expr.getRight()
                   : expr.getLeft();
        }
        else if (parent instanceof JetUnaryExpression && ((JetUnaryExpression) parent).getOperationReference() == this) {
            return ((JetUnaryExpression) parent).getBaseExpression();
        }
        else if (parent instanceof JetUserType) {
            JetUserType qualifier = ((JetUserType) parent).getQualifier();
            if (qualifier != null) {
                return qualifier.getReferenceExpression();
            }
        }
        return null;
    }

    public boolean isImportDirectiveExpression() {
        PsiElement parent = getParent();
        if (parent == null) return false;
        else return parent instanceof JetImportDirective ||
                    parent.getParent() instanceof JetImportDirective;
    }

    @NotNull
    public String getReferencedName() {
        String text = getReferencedNameElement().getNode().getText();
        return JetPsiUtil.unquoteIdentifierOrFieldReference(text);
    }

    @NotNull
    public Name getReferencedNameAsName() {
        String name = getReferencedName();
        return Name.identifierNoValidate(name);
    }

    @NotNull
    public PsiElement getReferencedNameElement() {
        PsiElement element = findChildByType(REFERENCE_TOKENS);
        if (element == null) {
            element = findChildByType(JetExpressionParsing.ALL_OPERATIONS);
        }

        if (element != null) {
            return element;
        }

        return this;
    }

    @Nullable
    public PsiElement getIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @Nullable @IfNotParsed
    public IElementType getReferencedNameElementType() {
        return getReferencedNameElement().getNode().getElementType();
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSimpleNameExpression(this, data);
    }
}

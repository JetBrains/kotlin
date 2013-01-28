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
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetSimpleNameExpression extends JetReferenceExpression {
    public static final TokenSet REFERENCE_TOKENS = TokenSet.orSet(LABELS, TokenSet.create(IDENTIFIER, FIELD_IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD));

    public JetSimpleNameExpression(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * null if it's not a code expression
     * @return receiver expression
     */
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
                return qualifiedExpression.getReceiverExpression();
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

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @Nullable
    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        if (references.length == 1) return references[0];
        else return null;
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitSimpleNameExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSimpleNameExpression(this, data);
    }
}

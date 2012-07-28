/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/**
 * @author max
 */
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
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) parent;
            if (!isFirstPartInQualifiedExpression(qualifiedExpression)) {
                return qualifiedExpression.getReceiverExpression();
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

    // Check that this is simple name expression is first part in full qualified name: firstPart.otherPart.otherPart.call()
    private boolean isFirstPartInQualifiedExpression(JetQualifiedExpression qualifiedExpression) {
        if (qualifiedExpression.getParent() instanceof JetQualifiedExpression) {
            return isFirstPartInQualifiedExpression((JetQualifiedExpression) qualifiedExpression.getParent());
        }

        return qualifiedExpression.getFirstChild() == this;
    }

    public boolean isImportDirectiveExpression() {
        PsiElement parent = getParent();
        if (parent == null) return false;
        else return parent instanceof JetImportDirective ||
                    parent.getParent() instanceof JetImportDirective;
    }

    @Nullable @IfNotParsed
    public String getReferencedName() {
        String text = getReferencedNameElement().getNode().getText();
        return text != null ? JetPsiUtil.unquoteIdentifierOrFieldReference(text) : null;
    }

    public Name getReferencedNameAsName() {
        String name = getReferencedName();
        if (name != null && name.length() == 0) {
            // TODO: fix parser or do something // stepan.koltsov@
        }
        return name != null ? Name.identifierNoValidate(name) : null;
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

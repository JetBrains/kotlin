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
            return qualifiedExpression.getReceiverExpression();
        } else if (parent instanceof JetCallExpression) {
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

    @Nullable @IfNotParsed
    public String getReferencedName() {
        PsiElement referencedNameElement = getReferencedNameElement();
        if (referencedNameElement == null) {
            return null;
        }
        String text = referencedNameElement.getNode().getText();
        return text != null ? JetPsiUtil.unquoteIdentifierOrFieldReference(text) : null;
    }

    @Nullable @IfNotParsed
    public PsiElement getReferencedNameElement() {
        PsiElement element = findChildByType(REFERENCE_TOKENS);
        if (element == null) {
            element = findChildByType(JetExpressionParsing.ALL_OPERATIONS);
        }
        return element;
    }

    @Nullable @IfNotParsed
    public IElementType getReferencedNameElementType() {
        PsiElement element = getReferencedNameElement();
        return element == null ? null : element.getNode().getElementType();
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
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

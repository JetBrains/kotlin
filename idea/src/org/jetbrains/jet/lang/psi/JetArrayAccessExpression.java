package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetArrayAccessExpression extends JetReferenceExpression {
    public JetArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitArrayAccessExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitArrayAccessExpression(this, data);
    }

    @NotNull
    public JetExpression getArrayExpression() {
        JetExpression baseExpression = findChildByClass(JetExpression.class);
        assert baseExpression != null;
        return baseExpression;
    }

    @NotNull
    public List<JetExpression> getIndexExpressions() {
        PsiElement container = getIndicesNode();
        if (container == null) return Collections.emptyList();
        return PsiTreeUtil.getChildrenOfTypeAsList(container, JetExpression.class);
    }

    public JetContainerNode getIndicesNode() {
        return (JetContainerNode) findChildByType(JetNodeTypes.INDICES);
    }
}

package org.jetbrains.jet.lang.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetUserType extends JetTypeElement {
    public JetUserType(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isAbsoluteInRootNamespace() {
        return findChildByType(JetTokens.NAMESPACE_KEYWORD) != null;
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitUserType(this);
    }

    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        // TODO: empty elements in PSI
        JetTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.<JetTypeProjection>emptyList() : typeArgumentList.getArguments();
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        List<JetTypeReference> result = Lists.newArrayList();
        for (JetTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }

    @Nullable @IfNotParsed
    public JetSimpleNameExpression getReferenceExpression() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable
    public JetUserType getQualifier() {
        return (JetUserType) findChildByType(JetNodeTypes.USER_TYPE);
    }

    @Nullable
    public String getReferencedName() {
        JetSimpleNameExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}

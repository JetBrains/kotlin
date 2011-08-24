package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetAnnotationEntry extends JetElement implements JetCallElement {
    public JetAnnotationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitAnnotationEntry(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotationEntry(this, data);
    }


    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        JetConstructorCalleeExpression calleeExpression = getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }
        return calleeExpression.getTypeReference();
    }

    @Override
    public JetConstructorCalleeExpression getCalleeExpression() {
        return (JetConstructorCalleeExpression) findChildByType(JetNodeTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    @Override
    public List<? extends ValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<JetExpression> getFunctionLiteralArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public JetTypeArgumentList getTypeArgumentList() {
        JetTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            return userType.getTypeArgumentList();
        }
        return null;
    }

}

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public class JetConstructorCalleeExpression extends JetExpression {
    public JetConstructorCalleeExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        return findChildByClass(JetTypeReference.class);
    }

    @Nullable @IfNotParsed
    public JetReferenceExpression getConstructorReferenceExpression() {
        JetTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) {
            return null;
        }
        assert typeElement instanceof JetUserType : typeElement;
        return ((JetUserType) typeElement).getReferenceExpression();
    }

}

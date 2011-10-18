package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetClassObject extends JetDeclaration {
    public JetClassObject(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitClassObject(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitClassObject(this, data);
    }

    @Nullable @IfNotParsed
    public JetObjectDeclaration getObjectDeclaration() {
        return (JetObjectDeclaration) findChildByType(JetNodeTypes.OBJECT_DECLARATION);
    }

}

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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitClassObject(this);
    }

    @Nullable @IfNotParsed
    public JetObjectDeclaration getObjectDeclaration() {
        return (JetObjectDeclaration) findChildByType(JetNodeTypes.OBJECT_DECLARATION);
    }

}

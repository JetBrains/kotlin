package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetDelegatorToSuperClass extends JetDelegationSpecifier {
    public JetDelegatorToSuperClass(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationToSuperClassSpecifier(this);
    }
}

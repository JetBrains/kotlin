package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class JetWhenCondition extends JetElement {
    public JetWhenCondition(@NotNull ASTNode node) {
        super(node);
    }
}

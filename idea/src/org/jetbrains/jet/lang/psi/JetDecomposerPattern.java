package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetDecomposerPattern extends JetPattern {
    public JetDecomposerPattern(@NotNull ASTNode node) {
        super(node);
    }
}

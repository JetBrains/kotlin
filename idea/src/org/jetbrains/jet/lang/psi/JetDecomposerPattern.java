package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public class JetDecomposerPattern extends JetPattern {
    public JetDecomposerPattern(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public JetExpression getDecomposerExpression() {
        return findChildByClass(JetExpression.class);
    }

    public JetTuplePattern getArgumentList() {
        return (JetTuplePattern) findChildByType(JetNodeTypes.DECOMPOSER_ARGUMENT_LIST);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitDecomposerPattern(this);
    }
}

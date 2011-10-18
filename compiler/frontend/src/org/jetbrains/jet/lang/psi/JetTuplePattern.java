package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author abreslav
 */
public class JetTuplePattern extends JetPattern {
    public JetTuplePattern(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTuplePattern(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTuplePattern(this, data);
    }

    @NotNull
    public List<JetTuplePatternEntry> getEntries() {
        return findChildrenByType(JetNodeTypes.TUPLE_PATTERN_ENTRY);
    }
}

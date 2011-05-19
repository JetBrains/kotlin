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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTuplePattern(this);
    }

    @NotNull
    public List<JetTuplePatternEntry> getEntries() {
        return findChildrenByType(JetNodeTypes.TUPLE_PATTERN_ENTRY);
    }
}

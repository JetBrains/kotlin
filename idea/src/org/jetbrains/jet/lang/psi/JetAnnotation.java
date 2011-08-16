package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetAnnotation extends JetElement {
    public JetAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitAnnotation(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotation(this, data);
    }

    public List<JetAnnotationEntry> getEntries() {
        return findChildrenByType(JetNodeTypes.ANNOTATION_ENTRY);
    }
}

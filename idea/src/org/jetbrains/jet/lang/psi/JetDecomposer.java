package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetDecomposer extends JetNamedDeclaration {
    public JetDecomposer(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitDecomposer(this);
    }

    @Nullable @IfNotParsed
    public JetDecomposerPropertyList getPropertyList() {
        return (JetDecomposerPropertyList) findChildByType(JetNodeTypes.DECOMPOSER_PROPERTY_LIST);
    }

    @NotNull
    public List<JetReferenceExpression> getPropertyReferences() {
        JetDecomposerPropertyList list = getPropertyList();

        return list != null ? list.getPropertyReferences() : Collections.<JetReferenceExpression>emptyList();
    }
}

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
public class JetClass extends JetTypeParameterListOwner {
    public JetClass(@NotNull ASTNode node) {
        super(node);
    }

    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitClass(this);
    }

    @Nullable
    public JetParameterList getPrimaryConstructorParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getPrimaryConstructorParameters() {
        JetParameterList list = getPrimaryConstructorParameterList();
        if (list == null) return Collections.emptyList();
        return list.getParameters();
    }
}

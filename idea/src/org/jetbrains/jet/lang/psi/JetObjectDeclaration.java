package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class JetObjectDeclaration extends JetNamedDeclaration implements JetClassOrObject  {
    public JetObjectDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public JetDelegationSpecifierList getDelegationSpecifierList() {
        return (JetDelegationSpecifierList) findChildByType(JetNodeTypes.DELEGATION_SPECIFIER_LIST);
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        JetDelegationSpecifierList list = getDelegationSpecifierList();
        return list != null ? list.getDelegationSpecifiers() : Collections.<JetDelegationSpecifier>emptyList();
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }
}

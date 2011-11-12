package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetClass extends JetTypeParameterListOwner implements JetClassOrObject, JetModifierListOwner {
    public JetClass(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @NotNull
    public List<JetSecondaryConstructor> getSecondaryConstructors() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getSecondaryConstructors();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitClass(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
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

    @Nullable
    public JetModifierList getPrimaryConstructorModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST);
    }

    @NotNull
    public List<JetClassInitializer> getAnonymousInitializers() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getAnonymousInitializers();
    }

    public boolean hasPrimaryConstructor() {
        return getPrimaryConstructorParameterList() != null;
    }

    @Override
    public JetObjectDeclarationName getNameAsDeclaration() {
        return (JetObjectDeclarationName) findChildByType(JetNodeTypes.OBJECT_DECLARATION_NAME);
    }

    @Override
    public JetClassBody getBody() {
        return (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
    }

    @Nullable
    public JetClassObject getClassObject() {
        JetClassBody body = getBody();
        if (body == null) return null;
        return body.getClassObject();
    }

    public List<JetProperty> getProperties() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getProperties();
    }

    public boolean isTrait() {
        return findChildByType(JetTokens.TRAIT_KEYWORD) != null;
    }
}

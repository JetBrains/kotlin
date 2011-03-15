package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class ScopeWithReceiver extends JetScopeImpl {

    private final JetScope receiverTypeScope;
    private final JetScope outerScope;

    public ScopeWithReceiver(JetScope outerScope, JetType receiverType) {
        this.outerScope = outerScope;
        this.receiverTypeScope = receiverType.getMemberScope();
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        return super.getClass(name); // TODO
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        return receiverTypeScope.getProperty(name);
        // TODO : extension properties
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        return super.getExtension(name); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return receiverTypeScope.getNamespace(name);
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        return outerScope.getTypeParameter(name);
    }

    @NotNull
    @Override
    public JetType getThisType() {
        return receiverTypeScope.getThisType();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return outerScope.getContainingDeclaration();
    }
}

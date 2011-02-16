package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class ScopeWithReceiver extends JetScopeImpl {

    private final JetScope receiverTypeScope;
    private final JetScope outerScope;

    public ScopeWithReceiver(JetScope outerScope, Type receiverType) {
        this.outerScope = outerScope;
        this.receiverTypeScope = receiverType.getMemberScope();
    }

    @Override
    public ClassDescriptor getClass(String name) {
        return super.getClass(name); // TODO
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        return receiverTypeScope.getProperty(name);
        // TODO : extension properties
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        return super.getExtension(name); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        return outerScope.getNamespace(name);
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        return outerScope.getTypeParameter(name);
    }

    @NotNull
    @Override
    public Type getThisType() {
        return receiverTypeScope.getThisType();
    }
}

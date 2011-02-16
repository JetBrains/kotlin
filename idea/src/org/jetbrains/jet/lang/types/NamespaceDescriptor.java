package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.modules.NamespaceDomain;

import java.util.Collection;

/**
 * @author abreslav
 */
public class NamespaceDescriptor implements NamespaceDomain {
    public ClassDescriptor getClass(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    public Collection<FunctionDescriptor> getMethods(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    public PropertyDescriptor getProperty(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String namespaceName) {
        throw new UnsupportedOperationException(); // TODO
    }
}

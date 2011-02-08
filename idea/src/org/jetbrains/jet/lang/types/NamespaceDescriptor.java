package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.modules.MemberDomain;
import org.jetbrains.jet.lang.modules.NamespaceDomain;

import java.util.Collection;

/**
 * @author abreslav
 */
public class NamespaceDescriptor implements NamespaceDomain, MemberDomain {
    @Override
    public ClassDescriptor getClass(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Collection<MethodDescriptor> getMethods(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String namespaceName) {
        throw new UnsupportedOperationException(); // TODO
    }
}

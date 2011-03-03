package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public class NamespaceDescriptor extends DeclarationDescriptorImpl {
    private NamespaceType namespaceType;
    private final JetScope memberScope;

    public NamespaceDescriptor(List<Attribute> attributes, String name, JetScope memberScope) {
        super(attributes, name);
        this.memberScope = memberScope;
    }

    public JetScope getMemberScope() {
        return memberScope;
    }

    public NamespaceType getNamespaceType() {
        if (namespaceType == null) {
            namespaceType = new NamespaceType(getName(), memberScope);
        }
        return namespaceType;
    }
}

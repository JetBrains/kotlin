package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;

import java.util.Map;

/**
 * @author abreslav
 */
public class TopDownAnalysisContext {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    public Map<JetNamespace, WritableScope> getNamespaceScopes() {
        return namespaceScopes;
    }

    public Map<JetNamespace, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
    }


}

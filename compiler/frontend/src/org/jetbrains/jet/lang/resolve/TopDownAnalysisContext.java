package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
/*package*/ class TopDownAnalysisContext {

    private final ObservableBindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetNamedFunction, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();
    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();

    public TopDownAnalysisContext(JetSemanticServices semanticServices, BindingTrace trace) {
        this.trace = new ObservableBindingTrace(trace);
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
    }

    public ObservableBindingTrace getTrace() {
        return trace;
    }

    public JetSemanticServices getSemanticServices() {
        return semanticServices;
    }

    public ClassDescriptorResolver getClassDescriptorResolver() {
        return classDescriptorResolver;
    }

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

    public Set<PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    public Map<JetDeclaration, ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    public Map<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    public Map<JetNamedFunction, FunctionDescriptorImpl> getFunctions() {
        return functions;
    }

}

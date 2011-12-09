package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;

/**
 * @author abreslav
 */
public class JavaSemanticServices {
    private final JavaTypeTransformer typeTransformer;
    private final JavaDescriptorResolver descriptorResolver;
    private final BindingTrace trace;
    private final JetSemanticServices jetSemanticServices;

    public JavaSemanticServices(Project project, JetSemanticServices jetSemanticServices, BindingTrace trace) {
        this.trace = trace;
        this.descriptorResolver = new JavaDescriptorResolver(project, this);
        this.typeTransformer = new JavaTypeTransformer(jetSemanticServices.getStandardLibrary(), descriptorResolver);
        this.jetSemanticServices = jetSemanticServices;
    }

    @NotNull
    public JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    @NotNull
    public JavaDescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @NotNull
    public BindingTrace getTrace() {
        return trace;
    }

    public JetSemanticServices getJetSemanticServices() {
        return jetSemanticServices;
    }
    
    @Nullable
    public ClassDescriptor getKotlinClassDescriptor(String qualifiedName) {
        return getTrace().get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qualifiedName);
    }

    @Nullable
    public NamespaceDescriptor getKotlinNamespaceDescriptor(String qualifiedName) {
        return getTrace().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, qualifiedName);
    }
}

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
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
}

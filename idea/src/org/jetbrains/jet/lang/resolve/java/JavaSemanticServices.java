package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.types.BindingTrace;

/**
 * @author abreslav
 */
public class JavaSemanticServices {
    private final JavaTypeTransformer typeTransformer;
    private final JavaDescriptorResolver descriptorResolver;
    private final BindingTrace trace;

    public JavaSemanticServices(Project project, JetSemanticServices jetSemanticServices, BindingTrace trace) {
        this.trace = trace;
        this.descriptorResolver = new JavaDescriptorResolver(project, this);
        this.typeTransformer = new JavaTypeTransformer(jetSemanticServices.getStandardLibrary(), descriptorResolver);
    }

    @NotNull
    public JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public JavaDescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    public BindingTrace getTrace() {
        return trace;
    }
}

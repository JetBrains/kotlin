package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.types.BindingTrace;
import org.jetbrains.jet.lang.types.JetTypeChecker;

/**
 * @author abreslav
 */
public class JavaSemanticServices {
    private final JavaTypeTransformer typeTransformer;
    private final JavaDescriptorResolver descriptorResolver;
    private final BindingTrace trace;
    private final JetTypeChecker typeChecker;

    public JavaSemanticServices(Project project, JetSemanticServices jetSemanticServices, BindingTrace trace) {
        this.trace = trace;
        this.descriptorResolver = new JavaDescriptorResolver(project, this);
        this.typeTransformer = new JavaTypeTransformer(jetSemanticServices.getStandardLibrary(), descriptorResolver);
        this.typeChecker = new JetTypeChecker(jetSemanticServices.getStandardLibrary());
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

    @NotNull
    public JetTypeChecker getTypeChecker() {
        return typeChecker;
    }
}

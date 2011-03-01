package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.types.BindingTrace;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetTypeInferrer;

/**
 * @author abreslav
 */
public class JetSemanticServices {
    public static JetSemanticServices createSemanticServices(JetStandardLibrary standardLibrary, ErrorHandler errorHandler) {
        return new JetSemanticServices(standardLibrary, errorHandler);
    }

    public static JetSemanticServices createSemanticServices(Project project, ErrorHandler errorHandler) {
        return new JetSemanticServices(new JetStandardLibrary(project), errorHandler);
    }

    private final JetTypeInferrer typeInferrer;
    private final JetStandardLibrary standardLibrary;

    private final ErrorHandler errorHandler;

    private JetSemanticServices(JetStandardLibrary standardLibrary, ErrorHandler errorHandler) {
        this.standardLibrary = standardLibrary;
        this.errorHandler = errorHandler;
        this.typeInferrer = new JetTypeInferrer(BindingTrace.DUMMY, this);
    }

    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    public JetTypeInferrer getTypeInferrer() {
        return typeInferrer;
    }

    public ClassDescriptorResolver getClassDescriptorResolver(BindingTrace trace) {
        return new ClassDescriptorResolver(this, trace);
    }

    public JetTypeInferrer getTypeInferrer(BindingTrace trace) {
        return new JetTypeInferrer(trace, this);
    }
}

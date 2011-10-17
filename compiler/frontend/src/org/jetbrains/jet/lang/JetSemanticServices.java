package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class JetSemanticServices {
    public static JetSemanticServices createSemanticServices(JetStandardLibrary standardLibrary) {
        return new JetSemanticServices(standardLibrary, JetControlFlowDataTraceFactory.EMPTY);
    }

    public static JetSemanticServices createSemanticServices(Project project) {
        return new JetSemanticServices(JetStandardLibrary.getJetStandardLibrary(project), JetControlFlowDataTraceFactory.EMPTY);
    }

    private final JetStandardLibrary standardLibrary;
    private final JetTypeChecker typeChecker;

    private JetSemanticServices(JetStandardLibrary standardLibrary, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        this.standardLibrary = standardLibrary;
        this.typeChecker = new JetTypeChecker(standardLibrary);
    }

    @NotNull
    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    @NotNull
    public ClassDescriptorResolver getClassDescriptorResolver(BindingTrace trace) {
        return new ClassDescriptorResolver(this, trace);
    }

    @NotNull
    public JetTypeInferrer.Services getTypeInferrerServices(@NotNull BindingTrace trace) {
        return new JetTypeInferrer(this).getServices(trace);
    }

    @NotNull
    public JetTypeChecker getTypeChecker() {
        return typeChecker;
    }

}
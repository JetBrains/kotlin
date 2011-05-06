package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class JetSemanticServices {
    public static JetSemanticServices createSemanticServices(JetStandardLibrary standardLibrary) {
        return new JetSemanticServices(standardLibrary);
    }

    public static JetSemanticServices createSemanticServices(Project project) {
        return new JetSemanticServices(JetStandardLibrary.getJetStandardLibrary(project));
    }

    private final JetStandardLibrary standardLibrary;
    private final JetTypeChecker typeChecker;
    private final OverloadResolver overloadResolver;


    private JetSemanticServices(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
        this.typeChecker = new JetTypeChecker(standardLibrary);
        this.overloadResolver = new OverloadResolver(typeChecker);
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
    public JetTypeInferrer getTypeInferrer(@NotNull BindingTrace trace, @NotNull JetFlowInformationProvider flowInformationProvider) {
        return new JetTypeInferrer(trace, flowInformationProvider, this);
    }

//    @NotNull
//    public ErrorHandler getErrorHandler() {
//        return errorHandler;
//    }
//
    @NotNull
    public JetTypeChecker getTypeChecker() {
        return typeChecker;
    }

    @NotNull
    public OverloadResolver getOverloadResolver() {
        return overloadResolver;
    }

}

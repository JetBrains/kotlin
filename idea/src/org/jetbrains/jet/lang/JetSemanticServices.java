package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
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

    public static JetSemanticServices createSemanticServices(Project project, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return new JetSemanticServices(JetStandardLibrary.getJetStandardLibrary(project), flowDataTraceFactory);
    }

    private final JetStandardLibrary standardLibrary;
    private final JetTypeChecker typeChecker;
    private final OverloadResolver overloadResolver;
    private final JetControlFlowDataTraceFactory flowDataTraceFactory;


    private JetSemanticServices(JetStandardLibrary standardLibrary, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        this.standardLibrary = standardLibrary;
        this.typeChecker = new JetTypeChecker(standardLibrary);
        this.overloadResolver = new OverloadResolver(typeChecker);
        this.flowDataTraceFactory = flowDataTraceFactory;
    }

    @NotNull
    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    @NotNull
    public ClassDescriptorResolver getClassDescriptorResolver(BindingTrace trace) {
        return new ClassDescriptorResolver(this, trace, flowDataTraceFactory);
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

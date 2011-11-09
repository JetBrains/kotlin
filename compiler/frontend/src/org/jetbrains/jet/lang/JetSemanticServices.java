package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

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

    private JetSemanticServices(JetStandardLibrary standardLibrary) {
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
    public ExpressionTypingServices getTypeInferrerServices(@NotNull BindingTrace trace) {
        return new ExpressionTypingServices(this, trace);
    }

    @NotNull
    public JetTypeChecker getTypeChecker() {
        return typeChecker;
    }
}
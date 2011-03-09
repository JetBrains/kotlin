package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.java.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;

/**
 * @author abreslav
 */
public class AnalyzingUtils {
    public static BindingContext analyzeFile(@NotNull JetFile file, @NotNull ErrorHandler errorHandler) {
        JetNamespace rootNamespace = file.getRootNamespace();
        return analyzeNamespace(rootNamespace, errorHandler);
    }

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull ErrorHandler errorHandler) {
        Project project = namespace.getProject();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project, errorHandler);

        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JavaSemanticServices javaSemanticServices = new JavaSemanticServices(project, semanticServices, bindingTraceContext);

        WritableScope scope = new WritableScope(semanticServices.getStandardLibrary().getLibraryScope());
        scope.importScope(new JavaPackageScope("", javaSemanticServices));
        scope.importScope(new JavaPackageScope("java.lang", javaSemanticServices));

        new TopDownAnalyzer(semanticServices, bindingTraceContext).process(scope, namespace);
        return bindingTraceContext;
    }
}

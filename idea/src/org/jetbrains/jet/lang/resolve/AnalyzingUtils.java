package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.java.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.types.ModuleDescriptor;

/**
 * @author abreslav
 */
public class AnalyzingUtils {
    public static void checkForSyntacticErrors(@NotNull PsiElement root) {
        root.acceptChildren(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }

            @Override
            public void visitErrorElement(PsiErrorElement element) {
                throw new IllegalArgumentException(element.getErrorDescription() + " at offset " + element.getTextRange().getStartOffset());
            }
        });
    }

    public static BindingContext analyzeFile(@NotNull JetFile file, @NotNull ErrorHandler errorHandler) {
        JetNamespace rootNamespace = file.getRootNamespace();
        return analyzeNamespace(rootNamespace, errorHandler, JetControlFlowDataTraceFactory.EMPTY);
    }

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull ErrorHandler errorHandler) {
        return analyzeNamespace(namespace, errorHandler, JetControlFlowDataTraceFactory.EMPTY);
    }

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull ErrorHandler errorHandler, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        Project project = namespace.getProject();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project, errorHandler);

        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JavaSemanticServices javaSemanticServices = new JavaSemanticServices(project, semanticServices, bindingTraceContext);

        JetScope libraryScope = semanticServices.getStandardLibrary().getLibraryScope();
        WritableScope scope = semanticServices.createWritableScope(libraryScope, new ModuleDescriptor("<module>"));
//        scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("").getMemberScope());
//        scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("java.lang").getMemberScope());
        scope.importScope(new JavaPackageScope("", null, javaSemanticServices));
        scope.importScope(new JavaPackageScope("java.lang", null, javaSemanticServices));

        new TopDownAnalyzer(semanticServices, bindingTraceContext, flowDataTraceFactory).process(scope, namespace);
        return bindingTraceContext;
    }
}

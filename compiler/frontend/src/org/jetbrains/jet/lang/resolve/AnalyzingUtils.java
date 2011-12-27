package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
                throw new IllegalArgumentException(element.getErrorDescription() + "; looking at " + element.getNode().getElementType() + " '" + element.getText() + DiagnosticUtils.atLocation(element));
            }
        });
    }
    
    public static List<TextRange> getSyntaxErrorRanges(@NotNull PsiElement root) {
        final ArrayList<TextRange> r = new ArrayList<TextRange>();
        root.acceptChildren(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }

            @Override
            public void visitErrorElement(PsiErrorElement element) {
                r.add(element.getTextRange());
            }
        });
        return r;
    }

    public static void throwExceptionOnErrors(BindingContext bindingContext) {
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            DiagnosticHolder.THROW_EXCEPTION.report(diagnostic);
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------

    public static BindingContext analyzeFiles(
            @NotNull Project project,
            @NotNull Configuration configuration,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project);
        return analyzeFilesWithGivenTrace(project, configuration, files, filesToAnalyzeCompletely, flowDataTraceFactory, bindingTraceContext, semanticServices);
    }

    public static BindingContext analyzeFilesWithGivenTrace(
            @NotNull Project project,
            @NotNull Configuration configuration,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull BindingTraceContext bindingTraceContext,
            @NotNull JetSemanticServices semanticServices) {
        JetScope libraryScope = semanticServices.getStandardLibrary().getLibraryScope();
        ModuleDescriptor owner = new ModuleDescriptor("<module>");

        final WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, owner, new TraceBasedRedeclarationHandler(bindingTraceContext)).setDebugName("Root scope in analyzeNamespace");
//        configuration.addImports(project, semanticServices, bindingTraceContext, scope);
        scope.importScope(libraryScope);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        TopDownAnalyzer.process(semanticServices, bindingTraceContext, scope, new NamespaceLike.Adapter(owner) {

            private Map<String, NamespaceDescriptorImpl> declaredNamespaces = Maps.newHashMap();

            @Override
            public NamespaceDescriptorImpl getNamespace(String name) {
                return declaredNamespaces.get(name);
            }

            @Override
            public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                scope.addNamespace(namespaceDescriptor);
                declaredNamespaces.put(namespaceDescriptor.getName(), (NamespaceDescriptorImpl) namespaceDescriptor);
            }

            @Override
            public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {
                scope.addClassifierDescriptor(classDescriptor);
            }

            @Override
            public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
                scope.addFunctionDescriptor(functionDescriptor);
            }

            @Override
            public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                scope.addPropertyDescriptor(propertyDescriptor);
            }

            @Override
            public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                throw new IllegalStateException("Must be guaranteed not to happen by the parser");
            }
        }, files, filesToAnalyzeCompletely, flowDataTraceFactory, configuration);
        return bindingTraceContext.getBindingContext();
    }

}

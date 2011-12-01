package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import java.util.*;

/**
 * @author abreslav
 */
public class AnalyzingUtils {
    public static AnalyzingUtils getInstance(@NotNull ImportingStrategy importingStrategy) {
        return new AnalyzingUtils(importingStrategy);
    }

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

    private final ImportingStrategy importingStrategy;

    private AnalyzingUtils(ImportingStrategy importingStrategy) {
        this.importingStrategy = importingStrategy;
    }

    public BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        Project project = namespace.getProject();
        List<JetDeclaration> declarations = Collections.<JetDeclaration>singletonList(namespace);

        return analyzeNamespaces(project, declarations, Predicates.equalTo(namespace.getContainingFile()), flowDataTraceFactory);
    }

    public BindingContext analyzeNamespaces(
            @NotNull Project project,
            @NotNull Collection<? extends JetDeclaration> declarations,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project);

        JetScope libraryScope = semanticServices.getStandardLibrary().getLibraryScope();
        ModuleDescriptor owner = new ModuleDescriptor("<module>");

        final WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, owner, new TraceBasedRedeclarationHandler(bindingTraceContext)).setDebugName("Root scope in analyzeNamespace");
        importingStrategy.addImports(project, semanticServices, bindingTraceContext, scope);
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
                scope.addVariableDescriptor(propertyDescriptor);
            }

            @Override
            public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                throw new IllegalStateException("Must be guaranteed not to happen by the parser");
            }
        }, declarations, filesToAnalyzeCompletely, flowDataTraceFactory);
        return bindingTraceContext.getBindingContext();
    }

    public BindingContext shallowAnalyzeFiles(Collection<PsiFile> files) {
        assert files.size() > 0;
        
        Project project = files.iterator().next().getProject();

        Collection<JetNamespace> namespaces = collectRootNamespaces(files);

        return analyzeNamespaces(project, namespaces, Predicates.<PsiFile>alwaysFalse(), JetControlFlowDataTraceFactory.EMPTY);
    }

    public static List<JetNamespace> collectRootNamespaces(Collection<PsiFile> files) {
        List<JetNamespace> namespaces = new ArrayList<JetNamespace>();

        for (PsiFile file : files) {
            if (file instanceof JetFile) {
                namespaces.add(((JetFile) file).getRootNamespace());
            }
        }
        return namespaces;
    }

}

package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import java.util.Collections;
import java.util.List;

//import org.jetbrains.jet.lang.resolve.java.JavaPackageScope;
//import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;

/**
 * @author abreslav
 */
public class AnalyzingUtils {
    private final static Key<CachedValue<BindingContext>> BINDING_CONTEXT = Key.create("BINDING_CONTEXT");
    private static final Object lock = new Object();

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

        return analyzeNamespaces(project, declarations, flowDataTraceFactory);
    }

    public BindingContext analyzeNamespaces(@NotNull Project project, @NotNull List<? extends JetDeclaration> declarations, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project, flowDataTraceFactory);

        JetScope libraryScope = semanticServices.getStandardLibrary().getLibraryScope();
        ModuleDescriptor owner = new ModuleDescriptor("<module>");
        final WritableScope scope = new WritableScopeImpl(libraryScope, owner, new TraceBasedRedeclarationHandler(bindingTraceContext)).setDebugName("Root scope in analyzeNamespace");
        importingStrategy.addImports(project, semanticServices, bindingTraceContext, scope);
        TopDownAnalyzer.process(semanticServices, bindingTraceContext, scope, new NamespaceLike.Adapter(owner) {

            @Override
            public NamespaceDescriptorImpl getNamespace(String name) {
                return null;
            }

            @Override
            public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                scope.addNamespace(namespaceDescriptor);
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
        }, declarations);
        return bindingTraceContext.getBindingContext();
    }

    public BindingContext analyzeFileWithCache(@NotNull final JetFile file) {
        // TODO : Synchronization?
        CachedValue<BindingContext> bindingContextCachedValue = file.getUserData(BINDING_CONTEXT);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<BindingContext>() {
                @Override
                public Result<BindingContext> compute() {
                    synchronized (lock) {
                        try {
                            JetNamespace rootNamespace = file.getRootNamespace();
                            BindingContext bindingContext = analyzeNamespace(rootNamespace, JetControlFlowDataTraceFactory.EMPTY);
                            return new Result<BindingContext>(bindingContext, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                        catch (ProcessCanceledException e) {
                            throw e;
                        }
                        catch (Throwable e) {
                            e.printStackTrace();
                            BindingTraceContext bindingTraceContext = new BindingTraceContext();
//                            bindingTraceContext.getErrorHandler().genericError(file.getNode(), e.getClass().getSimpleName() + ": " + e.getMessage());
                            bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
                            return new Result<BindingContext>(bindingTraceContext.getBindingContext(), PsiModificationTracker.MODIFICATION_COUNT);
                        }
                    }
                }
            }, false);
            file.putUserData(BINDING_CONTEXT, bindingContextCachedValue);
        }
        return bindingContextCachedValue.getValue();
    }
}

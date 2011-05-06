package org.jetbrains.jet.lang.resolve;

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
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetDiagnostic;
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
    private final static Key<CachedValue<BindingContext>> BINDING_CONTEXT = Key.create("BINDING_CONTEXT");

    public static BindingContext analyzeFileWithCache(@NotNull final JetFile file) {
        // TODO : Synchronization?
        CachedValue<BindingContext> bindingContextCachedValue = file.getUserData(BINDING_CONTEXT);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<BindingContext>() {
                @Override
                public Result<BindingContext> compute() {
                    JetNamespace rootNamespace = file.getRootNamespace();
                    BindingContext bindingContext = analyzeNamespace(rootNamespace, JetControlFlowDataTraceFactory.EMPTY);
                    return new Result<BindingContext>(bindingContext, PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);
            file.putUserData(BINDING_CONTEXT, bindingContextCachedValue);
        }
        return bindingContextCachedValue.getValue();
    }

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        Project project = namespace.getProject();

        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project);
        JavaSemanticServices javaSemanticServices = new JavaSemanticServices(project, semanticServices, bindingTraceContext);

        JetScope libraryScope = semanticServices.getStandardLibrary().getLibraryScope();
        WritableScope scope = new WritableScopeImpl(libraryScope, new ModuleDescriptor("<module>"), bindingTraceContext.getErrorHandler(), null);
//        scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("").getMemberScope());
//        scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("java.lang").getMemberScope());
        scope.importScope(new JavaPackageScope("", null, javaSemanticServices));
        scope.importScope(new JavaPackageScope("java.lang", null, javaSemanticServices));

        new TopDownAnalyzer(semanticServices, bindingTraceContext, flowDataTraceFactory).process(scope, namespace);
        return bindingTraceContext;
    }

    public static void applyHandler(@NotNull ErrorHandler errorHandler, @NotNull BindingContext bindingContext) {
        for (JetDiagnostic jetDiagnostic : bindingContext.getDiagnostics()) {
            jetDiagnostic.acceptHandler(errorHandler);
        }
    }

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
}

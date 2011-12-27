package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.StandardConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class AnalyzerFacade {

    public static final Function<JetFile, Collection<JetDeclaration>> SINGLE_DECLARATION_PROVIDER = new Function<JetFile, Collection<JetDeclaration>>() {
        @Override
        public Collection<JetDeclaration> fun(JetFile file) {
            return Collections.<JetDeclaration>singleton(file.getRootNamespace());
        }
    };

    private final static Key<CachedValue<BindingContext>> BINDING_CONTEXT = Key.create("BINDING_CONTEXT");
    private static final Object lock = new Object();

    public static BindingContext analyzeFileWithCache(@NotNull final JetFile file, @NotNull final Function<JetFile, Collection<JetDeclaration>> declarationProvider) {

        // Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {

            CachedValue<BindingContext> bindingContextCachedValue = file.getUserData(BINDING_CONTEXT);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<BindingContext>() {
                    @Override
                    public Result<BindingContext> compute() {
                        try {
                            BindingContext bindingContext = analyzeNamespacesWithJavaIntegration(file.getProject(), declarationProvider.fun(file), Predicates.<PsiFile>equalTo(file), JetControlFlowDataTraceFactory.EMPTY);
                            return new Result<BindingContext>(bindingContext, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                        catch (ProcessCanceledException e) {
                            throw e;
                        }
                        catch (Throwable e) {
                            DiagnosticUtils.throwIfRunningOnServer(e);

                            e.printStackTrace();
                            BindingTraceContext bindingTraceContext = new BindingTraceContext();
                            bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
                            return new Result<BindingContext>(bindingTraceContext.getBindingContext(), PsiModificationTracker.MODIFICATION_COUNT);
                        }
                    }
                }, false);
                file.putUserData(BINDING_CONTEXT, bindingContextCachedValue);
            }

            return bindingContextCachedValue.getValue();
        }
    }

    public static BindingContext analyzeOneNamespaceWithJavaIntegration(JetNamespace namespace, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return analyzeNamespacesWithJavaIntegration(namespace.getProject(), Collections.singleton(namespace), Predicates.<PsiFile>alwaysTrue(), flowDataTraceFactory);
    }

    public static BindingContext analyzeNamespacesWithJavaIntegration(Project project, Collection<? extends JetDeclaration> declarations, Predicate<PsiFile> filesToAnalyzeCompletely, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        JetSemanticServices semanticServices = JetSemanticServices.createSemanticServices(project);
        return AnalyzingUtils.analyzeNamespacesWithGivenTrace(
                project,
                JavaBridgeConfiguration.createJavaBridgeConfiguration(project, bindingTraceContext, StandardConfiguration.createStandardConfiguration(project)),
                declarations,
                filesToAnalyzeCompletely,
                flowDataTraceFactory,
                bindingTraceContext,
                semanticServices);
    }

    public static BindingContext shallowAnalyzeFiles(Collection<PsiFile> files) {
        assert files.size() > 0;

        Project project = files.iterator().next().getProject();

        Collection<JetNamespace> namespaces = collectRootNamespaces(files);

        return analyzeNamespacesWithJavaIntegration(project, namespaces, Predicates.<PsiFile>alwaysFalse(), JetControlFlowDataTraceFactory.EMPTY);
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

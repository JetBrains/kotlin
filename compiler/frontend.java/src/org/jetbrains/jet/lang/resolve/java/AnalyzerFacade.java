package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicates;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;

import java.util.Collection;
import java.util.Collections;

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
    
    private static final AnalyzingUtils ANALYZING_UTILS = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS);
    private final static Key<CachedValue<BindingContext>> BINDING_CONTEXT = Key.create("BINDING_CONTEXT");
    private static final Object lock = new Object();

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return ANALYZING_UTILS.analyzeNamespace(namespace, flowDataTraceFactory);
    }

    public static BindingContext analyzeFileWithCache(@NotNull final JetFile file, @NotNull final Function<JetFile, Collection<JetDeclaration>> declarationProvider) {
        return analyzeFileWithCache(ANALYZING_UTILS, file, declarationProvider);
    }

    public static BindingContext analyzeFileWithCache(@NotNull final AnalyzingUtils analyzingUtils, 
                                                      @NotNull final JetFile file, 
                                                      @NotNull final Function<JetFile, Collection<JetDeclaration>> declarationProvider) {
        // TODO : Synchronization?
        CachedValue<BindingContext> bindingContextCachedValue = file.getUserData(BINDING_CONTEXT);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<BindingContext>() {
                @Override
                public Result<BindingContext> compute() {
                    synchronized (lock) {
                        try {
                            BindingContext bindingContext = analyzingUtils.analyzeNamespaces(
                                    file.getProject(),
                                    declarationProvider.fun(file),
                                    Predicates.<PsiFile>equalTo(file),
                                    JetControlFlowDataTraceFactory.EMPTY);
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
                }

            }, false);
            file.putUserData(BINDING_CONTEXT, bindingContextCachedValue);
        }
        return bindingContextCachedValue.getValue();
    }
}

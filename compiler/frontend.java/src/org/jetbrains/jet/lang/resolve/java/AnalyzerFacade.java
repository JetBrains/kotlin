package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;

import java.util.Set;

/**
 * @author abreslav
 */
public class AnalyzerFacade {

    private static final AnalyzingUtils ANALYZING_UTILS = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS);
    private final static Key<CachedValue<BindingContext>> BINDING_CONTEXT = Key.create("BINDING_CONTEXT");
    private static final Object lock = new Object();

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return ANALYZING_UTILS.analyzeNamespace(namespace, flowDataTraceFactory);
    }

    public static BindingContext analyzeFileWithCache(@NotNull final JetFile file) {
        return analyzeFileWithCache(ANALYZING_UTILS, file);
    }

    public static BindingContext analyzeFileWithCache(@NotNull final AnalyzingUtils analyzingUtils, @NotNull final JetFile file) {
        // TODO : Synchronization?
        CachedValue<BindingContext> bindingContextCachedValue = file.getUserData(BINDING_CONTEXT);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<BindingContext>() {
                @Override
                public Result<BindingContext> compute() {
                    synchronized (lock) {
                        final Project project = file.getProject();
                        final Set<JetDeclaration> namespaces = Sets.newLinkedHashSet();
                        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
                        if (rootManager != null && !ApplicationManager.getApplication().isUnitTestMode()) {
                            VirtualFile[] contentRoots = rootManager.getContentRoots();

                            CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor() {
                                @Override
                                protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
                                    if (!(file.getName().endsWith(".kt") || file.getName().endsWith(".kts"))) return;
                                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                                    if (psiFile instanceof JetFile) {
                                        namespaces.add(((JetFile) psiFile).getRootNamespace());
                                    }
                                }
                            });
                        }
                        else {
                            namespaces.add(file.getRootNamespace());
                        }
                        try {
                            BindingContext bindingContext = analyzingUtils.analyzeNamespaces(project, namespaces, JetControlFlowDataTraceFactory.EMPTY);
                            return new Result<BindingContext>(bindingContext, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                        catch (ProcessCanceledException e) {
                            throw e;
                        }
                        catch (Throwable e) {
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

/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.project;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.lazy.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.*;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeWithCache {

    private static final Logger LOG = Logger.getInstance("org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache");

    private final static Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST_HEADERS = Key.create("ANALYZE_EXHAUST_HEADERS");
    private final static Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST_FULL = Key.create("ANALYZE_EXHAUST_FULL");
    private final static Key<CachedValue<ResolveSession>> ANALYZE_EXHAUST_LAZY_FULL = Key.create("ANALYZE_EXHAUST_FULL");

    private static final Object lock = new Object();
    public static final Function<JetFile, Collection<JetFile>> SINGLE_DECLARATION_PROVIDER = new Function<JetFile, Collection<JetFile>>() {
        @Override
        public Collection<JetFile> fun(JetFile file) {
            return Collections.singleton(file);
        }
    };

    private AnalyzerFacadeWithCache() {
    }

    /**
     * Analyze project with string cache for given file. Given file will be fully analyzed.
     *
     * @param file
     * @param declarationProvider
     * @return
     */
    // TODO: Also need to pass several files when user have multi-file environment
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull final JetFile file,
                                                      @NotNull final Function<JetFile, Collection<JetFile>> declarationProvider) {
        // Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            CachedValue<AnalyzeExhaust> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST_FULL);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue =
                        CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                            @Override
                            public Result<AnalyzeExhaust> compute() {
                                try {
                                    // Collect context for headers first
                                    AnalyzeExhaust analyzeExhaustHeaders = analyzeHeadersWithCacheOnFile(file, declarationProvider);

                                    BodiesResolveContext context = analyzeExhaustHeaders.getBodiesResolveContext();
                                    assert context != null : "Headers resolver should prepare and stored information for bodies resolve";

                                    // Need to resolve bodies in given file and all in the same package
                                    AnalyzeExhaust exhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).analyzeBodiesInFiles(
                                            file.getProject(),
                                            Collections.<AnalyzerScriptParameter>emptyList(),
                                            new JetFilesProvider.SameJetFilePredicate(file),
                                            new DelegatingBindingTrace(analyzeExhaustHeaders.getBindingContext()),
                                            context);

                                    return new Result<AnalyzeExhaust>(exhaust, PsiModificationTracker.MODIFICATION_COUNT);
                                }
                                catch (ProcessCanceledException e) {
                                    throw e;
                                }
                                catch (Throwable e) {
                                    handleError(e);
                                    return emptyExhaustWithDiagnosticOnFile(e);
                                }
                            }

                            @NotNull
                            private Result<AnalyzeExhaust> emptyExhaustWithDiagnosticOnFile(Throwable e) {
                                BindingTraceContext bindingTraceContext = new BindingTraceContext();
                                bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
                                AnalyzeExhaust analyzeExhaust = AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e);

                                CachedValue<AnalyzeExhaust> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST_HEADERS);
                                if (bindingContextCachedValue != null && bindingContextCachedValue.hasUpToDateValue()) {
                                    // Force invalidating of headers cache - temp decision for monitoring rewrite slice bug
                                    PsiModificationTracker tracker = PsiManager.getInstance(file.getProject()).getModificationTracker();
                                    ((PsiModificationTrackerImpl) tracker).incOutOfCodeBlockModificationCounter();
                                }

                                return new Result<AnalyzeExhaust>(analyzeExhaust, PsiModificationTracker.MODIFICATION_COUNT);
                            }
                        }, false);

                file.putUserData(ANALYZE_EXHAUST_FULL, bindingContextCachedValue);
            }

            return bindingContextCachedValue.getValue();
        }
    }

    private static AnalyzeExhaust analyzeHeadersWithCacheOnFile(
            @NotNull final JetFile fileToCache,
            @NotNull final Function<JetFile, Collection<JetFile>> declarationProvider
    ) {
        CachedValue<AnalyzeExhaust> bindingContextCachedValue = fileToCache.getUserData(ANALYZE_EXHAUST_HEADERS);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue =
                    CachedValuesManager.getManager(fileToCache.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                        @Override
                        public Result<AnalyzeExhaust> compute() {
                            AnalyzeExhaust exhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(fileToCache)
                                    .analyzeFiles(fileToCache.getProject(),
                                                  declarationProvider.fun(fileToCache),
                                                  Collections.<AnalyzerScriptParameter>emptyList(),
                                                  Predicates.<PsiFile>alwaysFalse());

                            return new Result<AnalyzeExhaust>(exhaust, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                        }
                    }, false);
            fileToCache.putUserData(ANALYZE_EXHAUST_HEADERS, bindingContextCachedValue);
        }

        return bindingContextCachedValue.getValue();
    }

    private static void handleError(@NotNull Throwable e) {
        DiagnosticUtils.throwIfRunningOnServer(e);
        LOG.error(e);
    }

    private static final Object lazyLock = new Object();

    @NotNull
    public static ResolveSession getLazyResolveSession(@NotNull final JetFile file) {
        synchronized (lazyLock) {
            final Project fileProject = file.getProject();
            CachedValue<ResolveSession> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST_LAZY_FULL);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue =
                        CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<ResolveSession>() {
                            @Override
                            public Result<ResolveSession> compute() {
                                ModuleDescriptor javaModule = new ModuleDescriptor(Name.special("<java module>"));

                                InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(
                                        fileProject, new BindingTraceContext(), javaModule, BuiltinsScopeExtensionMode.ALL);

                                List<JetFile> files = JetFilesProvider.getInstance(fileProject).allInScope(GlobalSearchScope.allScope(fileProject));

                                // Given file can differ from the original because it can be a virtual copy with some modifications
                                JetFile originalFile = (JetFile) file.getOriginalFile();
                                files.remove(originalFile);
                                files.add(file);

                                final PsiClassFinder psiClassFinder = injector.getPsiClassFinder();

                                // TODO: Replace with stub declaration provider
                                final FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(files, new Predicate<FqName>() {
                                    @Override
                                    public boolean apply(FqName fqName) {
                                        return psiClassFinder.findPsiPackage(fqName) != null || new FqName("jet").equals(fqName);
                                    }
                                });

                                final JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

                                ModuleConfiguration moduleConfiguration = new ModuleConfiguration() {
                                    @Override
                                    public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
                                        final Collection<ImportPath> defaultImports = Lists.newArrayList(JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS);
                                        defaultImports.addAll(Arrays.asList(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS));

                                        for (ImportPath defaultJetImport : defaultImports) {
                                            directives.add(JetPsiFactory.createImportDirective(fileProject, defaultJetImport));
                                        }
                                    }

                                    @Override
                                    public void extendNamespaceScope(
                                            @NotNull BindingTrace trace,
                                            @NotNull NamespaceDescriptor namespaceDescriptor,
                                            @NotNull WritableScope namespaceMemberScope
                                    ) {
                                        FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
                                        if (new FqName("jet").equals(fqName)) {
                                            namespaceMemberScope.importScope(JetStandardLibrary.getInstance().getLibraryScope());
                                        }
                                        if (psiClassFinder.findPsiPackage(fqName) != null) {
                                            JavaPackageScope javaPackageScope = javaDescriptorResolver.getJavaPackageScope(fqName, namespaceDescriptor);
                                            namespaceMemberScope.importScope(javaPackageScope);
                                        }
                                    }
                                };

                                ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));
                                ResolveSession resolveSession = new ResolveSession(fileProject, lazyModule, moduleConfiguration, declarationProviderFactory);

                                return new Result<ResolveSession>(resolveSession, PsiModificationTracker.MODIFICATION_COUNT);
                            }
                        }, false);
                file.putUserData(ANALYZE_EXHAUST_LAZY_FULL, bindingContextCachedValue);
            }
            return bindingContextCachedValue.getValue();
        }
    }
}

/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.analyzer.AnalyzerFacadeForEverything;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public enum AnalyzerFacadeForJVM implements AnalyzerFacade {

    INSTANCE;

    private AnalyzerFacadeForJVM() {
    }

    @Override
    @NotNull
    public AnalyzeExhaust analyzeFiles(@NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull List<AnalyzerScriptParameter> scriptParameters,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        return analyzeFilesWithJavaIntegration(project, files, scriptParameters, filesToAnalyzeCompletely, true);
    }

    @NotNull
    @Override
    public AnalyzeExhaust analyzeBodiesInFiles(@NotNull Project project,
                                               @NotNull List<AnalyzerScriptParameter> scriptParameters,
                                               @NotNull Predicate<PsiFile> filesForBodiesResolve,
                                               @NotNull BindingTrace headersTraceContext,
                                               @NotNull BodiesResolveContext bodiesResolveContext,
                                               @NotNull ModuleSourcesManager moduleSourcesManager
    ) {
        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                project, scriptParameters, filesForBodiesResolve,
                headersTraceContext, bodiesResolveContext, moduleSourcesManager);
    }

    @NotNull
    @Override
    public ResolveSession getLazyResolveSession(@NotNull final Project fileProject, @NotNull Collection<JetFile> files) {
        MutableModuleDescriptor javaModule = new MutableModuleDescriptor(Name.special("<java module>"), JavaToKotlinClassMap.getInstance());
        SubModuleDescriptor subModule =
                javaModule.addSubModule(new MutableSubModuleDescriptor(javaModule, Name.special("<java submodule>")));

        BindingTraceContext javaResolverTrace = new BindingTraceContext();
        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(
                fileProject,
                javaResolverTrace,
                null, // TODO light class resolver
                new LockBasedStorageManager(),
                subModule,
                GlobalSearchScope.allScope(fileProject)
        );

        final PsiClassFinder psiClassFinder = null; // TODO

        // TODO: Replace with stub declaration provider
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        final FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(storageManager, files, new Predicate<FqName>() {
            @Override
            public boolean apply(FqName fqName) {
                return psiClassFinder.findPsiPackage(fqName) != null || new FqName("jet").equals(fqName);
            }
        });

        final JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        ModuleConfiguration moduleConfiguration = new ModuleConfiguration() {
            @Override
            public List<ImportPath> getDefaultImports() {
                LinkedHashSet<ImportPath> imports = Sets.newLinkedHashSet(JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS);
                imports.addAll(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);
                return Lists.newArrayList(imports);
            }

            @Override
            public void extendNamespaceScope(
                    @NotNull BindingTrace trace,
                    @NotNull PackageViewDescriptor namespaceDescriptor,
                    @NotNull WritableScope namespaceMemberScope
            ) {
                FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
                if (new FqName("jet").equals(fqName)) {
                    namespaceMemberScope.importScope(KotlinBuiltIns.getInstance().getBuiltInsScope());
                }
                if (psiClassFinder.findPsiPackage(fqName) != null) {
                    JetScope javaPackageScope = javaDescriptorResolver.getJavaPackageScope(namespaceDescriptor);
                    assert javaPackageScope != null;
                    namespaceMemberScope.importScope(javaPackageScope);
                }
            }

            @NotNull
            @Override
            public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
                return JavaToKotlinClassMap.getInstance();
            }
        };

        ModuleDescriptor lazyModule = new MutableModuleDescriptor(Name.special("<lazy module>"), JavaToKotlinClassMap.getInstance());

        return new ResolveSession(fileProject, storageManager, lazyModule, moduleConfiguration, declarationProviderFactory, javaResolverTrace);
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegrationAndCheckForErrors(
            JetFile file, List<AnalyzerScriptParameter> scriptParameters) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalyzeExhaust analyzeExhaust = analyzeOneFileWithJavaIntegration(file, scriptParameters);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegration(
            JetFile file, List<AnalyzerScriptParameter> scriptParameters) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file), scriptParameters,
                                               Predicates.<PsiFile>alwaysTrue());
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegrationAndCheckForErrors(
            Project project,
            Collection<JetFile> files,
            List<AnalyzerScriptParameter> scriptParameters,
            Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file); 
        }
       
        AnalyzeExhaust analyzeExhaust = analyzeFilesWithJavaIntegration(
                project, files, scriptParameters, filesToAnalyzeCompletely, false);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project,
            Collection<JetFile> files,
            List<AnalyzerScriptParameter> scriptParameters,
            Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        return analyzeFilesWithJavaIntegration(
                project, files, scriptParameters, filesToAnalyzeCompletely, false);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project, Collection<JetFile> files, List<AnalyzerScriptParameter> scriptParameters, Predicate<PsiFile> filesToAnalyzeCompletely,
            boolean storeContextForBodiesResolve) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        return analyzeFilesWithJavaIntegration(project, files, bindingTraceContext, scriptParameters, filesToAnalyzeCompletely,
                                               storeContextForBodiesResolve);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            List<AnalyzerScriptParameter> scriptParameters,
            Predicate<PsiFile> filesToAnalyzeCompletely,
            boolean storeContextForBodiesResolve
    ) {
        MutableModuleSourcesManager sourcesManager = new MutableModuleSourcesManager(project);
        MutableModuleDescriptor module = new MutableModuleDescriptor(Name.special("<module>"), JavaToKotlinClassMap.getInstance());
        MutableSubModuleDescriptor subModule = new MutableSubModuleDescriptor(module, Name.special("<submodule>"));
        module.addSubModule(subModule);
        for (JetFile file : files) {
            sourcesManager.registerRoot(subModule, PackageFragmentKind.SOURCE, file.getVirtualFile());
        }
        for (ImportPath path : JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS) {
            subModule.addDefaultImport(path);
        }
        for (ImportPath path : DefaultModuleConfiguration.DEFAULT_JET_IMPORTS) {
            subModule.addDefaultImport(path);
        }

        ObservableBindingTrace observableBindingTrace = new ObservableBindingTrace(trace);
        // TODO
        //subModule.addPackageFragmentProvider(new JavaPackageFragmentProvider(
        //        facade,
        //        trace,
        //        new LockBasedStorageManager(),
        //        new PsiDeclarationProviderFactory(finder),
        //        classResolver,
        //        finder,
        //        subModule
        //));

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, scriptParameters);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project, topDownAnalysisParameters,
                observableBindingTrace, KotlinModuleManager.SERVICE.getService(project).getSourcesManager());
        try {
            injector.getTopDownAnalyzer().analyzeFiles(files, scriptParameters);
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(trace.getBindingContext(), bodiesResolveContext, null /*get rid of ModuleConfiguration*/);
        } finally {
            injector.destroy();
        }
    }

    public static AnalyzeExhaust shallowAnalyzeFiles(Collection<JetFile> files) {
        assert files.size() > 0;

        Project project = files.iterator().next().getProject();

        return analyzeFilesWithJavaIntegration(project,
                files, Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysFalse());
    }
}

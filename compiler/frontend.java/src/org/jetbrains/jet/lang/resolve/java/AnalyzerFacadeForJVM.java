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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
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
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.lazy.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
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
                                               @NotNull ModuleConfiguration configuration
    ) {
        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                project, scriptParameters, filesForBodiesResolve,
                headersTraceContext, bodiesResolveContext, configuration);
    }

    @NotNull
    @Override
    public ResolveSession getLazyResolveSession(@NotNull final Project fileProject, @NotNull Collection<JetFile> files) {
        ModuleDescriptor javaModule = new ModuleDescriptor(Name.special("<java module>"));

        BindingTraceContext javaResolverTrace = new BindingTraceContext();
        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(fileProject, javaResolverTrace, javaModule);

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
                    namespaceMemberScope.importScope(KotlinBuiltIns.getInstance().getBuiltInsScope());
                }
                if (psiClassFinder.findPsiPackage(fqName) != null) {
                    JavaPackageScope javaPackageScope = javaDescriptorResolver.getJavaPackageScope(namespaceDescriptor);
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

        ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));

        return new ResolveSession(fileProject, lazyModule, moduleConfiguration, declarationProviderFactory, javaResolverTrace);
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

        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, scriptParameters);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(bindingTraceContext), owner);
        try {
            injector.getTopDownAnalyzer().analyzeFiles(files, scriptParameters);
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), bodiesResolveContext, injector.getModuleConfiguration());
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

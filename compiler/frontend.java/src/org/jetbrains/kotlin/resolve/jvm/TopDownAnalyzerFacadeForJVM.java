/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCacheProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum TopDownAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = buildDefaultImports();

    private static List<ImportPath> buildDefaultImports() {
        List<ImportPath> list = new ArrayList<ImportPath>();
        list.add(new ImportPath("java.lang.*"));
        list.add(new ImportPath("kotlin.*"));
        list.add(new ImportPath("kotlin.jvm.*"));
        list.add(new ImportPath("kotlin.io.*"));
        // all classes from package "kotlin" mapped to java classes are imported explicitly so that they take priority over classes from java.lang
        for (ClassDescriptor descriptor : JavaToKotlinClassMap.INSTANCE.allKotlinClasses()) {
            FqName fqName = DescriptorUtils.getFqNameSafe(descriptor);
            if (fqName.parent().equals(new FqName("kotlin"))) {
                list.add(new ImportPath(fqName, false));
            }
        }
        return list;
    }

    private TopDownAnalyzerFacadeForJVM() {
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationNoIncremental(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull ModuleDescriptorImpl module
    ) {
        return analyzeFilesWithJavaIntegration(project, files, trace, topDownAnalysisParameters, module, null, null);
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationWithCustomContext(
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull ModuleDescriptorImpl module,
            @Nullable List<String> moduleIds,
            @Nullable IncrementalCacheProvider incrementalCacheProvider
    ) {
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                false,
                false
        );

        return analyzeFilesWithJavaIntegration(
                project, files, trace, topDownAnalysisParameters, module,
                moduleIds, incrementalCacheProvider);
    }

    @NotNull
    private static AnalysisResult analyzeFilesWithJavaIntegration(
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            TopDownAnalysisParameters topDownAnalysisParameters,
            ModuleDescriptorImpl module,
            @Nullable List<String> moduleIds,
            @Nullable IncrementalCacheProvider incrementalCacheProvider
    ) {
        List<JetFile> allFiles = JvmAnalyzerFacade.getAllFilesToAnalyze(project, null, files);

        FileBasedDeclarationProviderFactory providerFactory =
                new FileBasedDeclarationProviderFactory(topDownAnalysisParameters.getStorageManager(), allFiles);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project,
                topDownAnalysisParameters,
                trace,
                module,
                providerFactory,
                GlobalSearchScope.allScope(project)
        );

        try {
            List<PackageFragmentProvider> additionalProviders = new ArrayList<PackageFragmentProvider>();

            if (moduleIds != null && incrementalCacheProvider != null) {
                for (String moduleId : moduleIds) {
                    IncrementalCache incrementalCache = incrementalCacheProvider.getIncrementalCache(moduleId);

                    additionalProviders.add(
                            new IncrementalPackageFragmentProvider(
                                    files, module, topDownAnalysisParameters.getStorageManager(),
                                    injector.getDeserializationComponentsForJava().getComponents(),
                                    incrementalCache, moduleId
                            )
                    );
                }
            }
            additionalProviders.add(injector.getJavaDescriptorResolver().getPackageFragmentProvider());

            injector.getLazyTopDownAnalyzerForTopLevel().analyzeFiles(topDownAnalysisParameters, allFiles, additionalProviders);
            return AnalysisResult.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJavaModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name),
                                        DEFAULT_IMPORTS,
                                        JavaToKotlinClassMap.INSTANCE);
    }

    @NotNull
    public static ModuleDescriptorImpl createSealedJavaModule() {
        ModuleDescriptorImpl module = createJavaModule("<shared-module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();
        return module;
    }
}

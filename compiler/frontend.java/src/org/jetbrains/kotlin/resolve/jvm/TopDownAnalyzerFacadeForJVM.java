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
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleParameters;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm;
import org.jetbrains.kotlin.frontend.java.di.DiPackage;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.context.ContextPackage.ContextForNewModule;

public enum TopDownAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = buildDefaultImports();

    private static List<ImportPath> buildDefaultImports() {
        List<ImportPath> list = new ArrayList<ImportPath>();
        list.add(new ImportPath("java.lang.*"));
        list.add(new ImportPath("kotlin.*"));
        list.add(new ImportPath("kotlin.annotation.*"));
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

    public static ModuleParameters JVM_MODULE_PARAMETERS = new ModuleParameters() {
        @NotNull
        @Override
        public List<ImportPath> getDefaultImports() {
            return DEFAULT_IMPORTS;
        }

        @NotNull
        @Override
        public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
            return JavaToKotlinClassMap.INSTANCE;
        }
    };

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationNoIncremental(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull TopDownAnalysisMode topDownAnalysisMode
    ) {
        return analyzeFilesWithJavaIntegration(moduleContext, files, trace, topDownAnalysisMode, null, null);
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationWithCustomContext(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @Nullable List<String> moduleIds,
            @Nullable IncrementalCompilationComponents incrementalCompilationComponents
    ) {
        return analyzeFilesWithJavaIntegration(
                moduleContext, files, trace, TopDownAnalysisMode.TopLevelDeclarations, moduleIds, incrementalCompilationComponents
        );
    }

    @NotNull
    private static AnalysisResult analyzeFilesWithJavaIntegration(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @Nullable List<String> moduleIds,
            @Nullable IncrementalCompilationComponents incrementalCompilationComponents
    ) {
        Project project = moduleContext.getProject();
        List<JetFile> allFiles = JvmAnalyzerFacade.getAllFilesToAnalyze(project, null, files);

        FileBasedDeclarationProviderFactory providerFactory =
                new FileBasedDeclarationProviderFactory(moduleContext.getStorageManager(), allFiles);

        LookupTracker lookupTracker =
                incrementalCompilationComponents != null ? incrementalCompilationComponents.getLookupTracker() : LookupTracker.DO_NOTHING;

        ContainerForTopDownAnalyzerForJvm container = DiPackage.createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory,
                GlobalSearchScope.allScope(project),
                lookupTracker
        );

        List<PackageFragmentProvider> additionalProviders = new ArrayList<PackageFragmentProvider>();

        if (moduleIds != null && incrementalCompilationComponents != null) {
            for (String moduleId : moduleIds) {
                IncrementalCache incrementalCache = incrementalCompilationComponents.getIncrementalCache(moduleId);

                additionalProviders.add(
                        new IncrementalPackageFragmentProvider(
                                files, moduleContext.getModule(), moduleContext.getStorageManager(),
                                container.getDeserializationComponentsForJava().getComponents(),
                                incrementalCache, moduleId
                        )
                );
            }
        }
        additionalProviders.add(container.getJavaDescriptorResolver().getPackageFragmentProvider());

        container.getLazyTopDownAnalyzerForTopLevel().analyzeFiles(topDownAnalysisMode, allFiles, additionalProviders);

        BindingContext bindingContext = trace.getBindingContext();
        ModuleDescriptor module = moduleContext.getModule();

        Collection<AnalysisCompletedHandlerExtension> analysisCompletedHandlerExtensions =
                AnalysisCompletedHandlerExtension.Companion.getInstances(moduleContext.getProject());

        for (AnalysisCompletedHandlerExtension extension : analysisCompletedHandlerExtensions) {
            AnalysisResult result = extension.analysisCompleted(project, module, bindingContext, files);
            if (result != null) return result;
        }

        return AnalysisResult.success(bindingContext, module);
    }

    @NotNull
    public static MutableModuleContext createContextWithSealedModule(@NotNull Project project) {
        MutableModuleContext context = ContextForNewModule(
                project, Name.special("<shared-module>"), JVM_MODULE_PARAMETERS
        );
        context.setDependencies(context.getModule(), KotlinBuiltIns.getInstance().getBuiltInsModule());
        return context;
    }
}

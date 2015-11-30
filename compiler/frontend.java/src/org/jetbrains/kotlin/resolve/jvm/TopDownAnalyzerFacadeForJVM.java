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
import org.jetbrains.kotlin.context.ContextKt;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.PackagePartProvider;
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm;
import org.jetbrains.kotlin.frontend.java.di.InjectionKt;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
import org.jetbrains.kotlin.modules.Module;
import org.jetbrains.kotlin.modules.TargetId;
import org.jetbrains.kotlin.modules.TargetIdKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode;
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension;
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum TopDownAnalyzerFacadeForJVM {

    INSTANCE;

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationNoIncremental(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<KtFile> files,
            @NotNull BindingTrace trace,
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            PackagePartProvider packagePartProvider
    ) {
        return analyzeFilesWithJavaIntegration(moduleContext, files, trace, topDownAnalysisMode, null, null, packagePartProvider);
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationWithCustomContext(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<KtFile> files,
            @NotNull BindingTrace trace,
            @Nullable List<Module> modules,
            @Nullable IncrementalCompilationComponents incrementalCompilationComponents,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        return analyzeFilesWithJavaIntegration(
                moduleContext, files, trace, TopDownAnalysisMode.TopLevelDeclarations, modules, incrementalCompilationComponents,
                packagePartProvider);
    }

    @NotNull
    private static AnalysisResult analyzeFilesWithJavaIntegration(
            @NotNull ModuleContext moduleContext,
            @NotNull Collection<KtFile> files,
            @NotNull BindingTrace trace,
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @Nullable List<Module> modules,
            @Nullable IncrementalCompilationComponents incrementalCompilationComponents,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        Project project = moduleContext.getProject();
        List<KtFile> allFiles = JvmAnalyzerFacade.getAllFilesToAnalyze(project, null, files);

        FileBasedDeclarationProviderFactory providerFactory =
                new FileBasedDeclarationProviderFactory(moduleContext.getStorageManager(), allFiles);

        LookupTracker lookupTracker =
                incrementalCompilationComponents != null ? incrementalCompilationComponents.getLookupTracker() : LookupTracker.Companion.getDO_NOTHING();

        List<TargetId> targetIds = null;
        if (modules != null) {
            targetIds = new ArrayList<TargetId>(modules.size());

            for (Module module : modules) {
                targetIds.add(TargetIdKt.TargetId(module));
            }
        }

        packagePartProvider = IncrementalPackagePartProvider.create(packagePartProvider, files, targetIds, incrementalCompilationComponents, moduleContext.getStorageManager());

        ContainerForTopDownAnalyzerForJvm container = InjectionKt.createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory,
                GlobalSearchScope.allScope(project),
                lookupTracker,
                packagePartProvider
        );

        List<PackageFragmentProvider> additionalProviders = new ArrayList<PackageFragmentProvider>();

        if (targetIds != null && incrementalCompilationComponents != null) {
            for (TargetId targetId : targetIds) {
                IncrementalCache incrementalCache = incrementalCompilationComponents.getIncrementalCache(targetId);

                additionalProviders.add(
                        new IncrementalPackageFragmentProvider(
                                files, moduleContext.getModule(), moduleContext.getStorageManager(),
                                container.getDeserializationComponentsForJava().getComponents(),
                                incrementalCache, targetId
                        )
                );
            }
        }
        additionalProviders.add(container.getJavaDescriptorResolver().getPackageFragmentProvider());

        for (PackageFragmentProviderExtension extension : PackageFragmentProviderExtension.Companion.getInstances(project)) {
            PackageFragmentProvider provider = extension.getPackageFragmentProvider(
                    project, moduleContext.getModule(), moduleContext.getStorageManager(), trace, null);
            if (provider != null) additionalProviders.add(provider);
        }

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
    public static MutableModuleContext createContextWithSealedModule(@NotNull Project project, @NotNull String moduleName) {
        MutableModuleContext context = ContextKt.ContextForNewModule(
                project, Name.special("<" + moduleName + ">"), JvmPlatform.INSTANCE$
        );
        context.setDependencies(context.getModule(), JvmPlatform.INSTANCE$.getBuiltIns().getBuiltInsModule());
        return context;
    }
}

/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.cache.IncrementalCache;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.cache.IncrementalCacheProvider;
import org.jetbrains.jet.lang.resolve.name.Name;

import org.jetbrains.jet.lang.resolve.android.AndroidPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum TopDownAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.jvm.*"),
            new ImportPath("kotlin.io.*")
    );

    private TopDownAnalyzerFacadeForJVM() {
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            Predicate<PsiFile> filesToAnalyzeCompletely,
            ModuleDescriptorImpl module,
            List<String> moduleIds,
            @Nullable IncrementalCacheProvider incrementalCacheProvider
    ) {
        GlobalContext globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                filesToAnalyzeCompletely,
                false,
                false
        );

        Collection<JetFile> filesToAnalyze = AndroidPackage.searchAndAddAndroidDeclarations(project, files);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(project, topDownAnalysisParameters, trace, module);
        try {
            List<PackageFragmentProvider> additionalProviders = new ArrayList<PackageFragmentProvider>();

            if (moduleIds != null && incrementalCacheProvider != null) {
                for (String moduleId : moduleIds) {
                    IncrementalCache incrementalCache = incrementalCacheProvider.getIncrementalCache(moduleId);

                    additionalProviders.add(
                            new IncrementalPackageFragmentProvider(
                                    filesToAnalyze, module, globalContext.getStorageManager(), injector.getDeserializationGlobalContextForJava(),
                                    incrementalCache, moduleId, injector.getJavaDescriptorResolver()
                            )
                    );
                }
            }
            additionalProviders.add(injector.getJavaDescriptorResolver().getPackageFragmentProvider());

            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, filesToAnalyze, additionalProviders);
            return AnalyzeExhaust.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJavaModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, JavaToKotlinClassMap.getInstance());
    }
}

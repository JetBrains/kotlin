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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    @NotNull
    public static ModuleDescriptor resolveProject(@NotNull Project project, @NotNull KotlinCoreEnvironment environment) {
        return resolve(project, Collections.<KtFile>emptyList(), environment);
    }

    @NotNull
    public static ModuleDescriptor resolve(@NotNull Project project, @NotNull List<KtFile> sourceFiles, @NotNull KotlinCoreEnvironment environment) {
        return resolve(project, new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(), sourceFiles, environment);
    }

    @NotNull
    public static ModuleDescriptor resolve(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull List<KtFile> sourceFiles,
            @NotNull KotlinCoreEnvironment environment
    ) {
        ModuleContext moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, JvmResolveUtil.TEST_MODULE_NAME);

        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationNoIncremental(
                moduleContext, sourceFiles, trace, TopDownAnalysisMode.TopLevelDeclarations,
                new JvmPackagePartProvider(environment)
        );

        return moduleContext.getModule();
    }

    @NotNull
    public static KotlinCodeAnalyzer resolveLazilyWithSession(
            @NotNull List<KtFile> files,
            @NotNull KotlinCoreEnvironment environment,
            boolean addBuiltIns
    ) {
        return LazyResolveTestUtilsKt.createResolveSessionForFiles(environment.getProject(), files, addBuiltIns);
    }

    public static ModuleDescriptor resolveLazily(List<KtFile> files, KotlinCoreEnvironment environment) {
        return resolveLazily(files, environment, true);
    }

    public static ModuleDescriptor resolveLazily(List<KtFile> files, KotlinCoreEnvironment environment, boolean addBuiltIns) {
        return resolveLazilyWithSession(files, environment, addBuiltIns).getModuleDescriptor();
    }

    @NotNull
    public static Set<Name> getTopLevelPackagesFromFileList(@NotNull List<KtFile> files) {
        Set<Name> shortNames = Sets.newLinkedHashSet();
        for (KtFile file : files) {
            List<Name> packageFqNameSegments = file.getPackageFqName().pathSegments();
            Name name = packageFqNameSegments.isEmpty() ? SpecialNames.ROOT_PACKAGE : packageFqNameSegments.get(0);
            shortNames.add(name);
        }
        return shortNames;
    }
}

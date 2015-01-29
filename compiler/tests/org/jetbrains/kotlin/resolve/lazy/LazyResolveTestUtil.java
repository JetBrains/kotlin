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
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.lazy.LazyPackage.createResolveSessionForFiles;

public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    @NotNull
    public static ModuleDescriptor resolveProject(@NotNull Project project) {
        return resolve(project, Collections.<JetFile>emptyList());
    }

    @NotNull
    public static ModuleDescriptor resolve(@NotNull Project project, @NotNull List<JetFile> sourceFiles) {
        return resolve(project, new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(), sourceFiles);
    }

    @NotNull
    public static ModuleDescriptor resolve(@NotNull Project project, @NotNull BindingTrace trace, @NotNull List<JetFile> sourceFiles) {
        ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJVM.createSealedJavaModule();

        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters params = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                false, false
        );

        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationNoIncremental(project, sourceFiles, trace, params, module);

        return module;
    }

    @NotNull
    public static KotlinCodeAnalyzer resolveLazilyWithSession(
            @NotNull List<JetFile> files,
            @NotNull JetCoreEnvironment environment,
            boolean addBuiltIns
    ) {
        return createResolveSessionForFiles(environment.getProject(), files, addBuiltIns);
    }

    public static ModuleDescriptor resolveLazily(List<JetFile> files, JetCoreEnvironment environment) {
        return resolveLazily(files, environment, true);
    }

    public static ModuleDescriptor resolveLazily(List<JetFile> files, JetCoreEnvironment environment, boolean addBuiltIns) {
        return resolveLazilyWithSession(files, environment, addBuiltIns).getModuleDescriptor();
    }

    @NotNull
    public static Set<Name> getTopLevelPackagesFromFileList(@NotNull List<JetFile> files) {
        Set<Name> shortNames = Sets.newLinkedHashSet();
        for (JetFile file : files) {
            List<Name> packageFqNameSegments = file.getPackageFqName().pathSegments();
            Name name = packageFqNameSegments.isEmpty() ? SpecialNames.ROOT_PACKAGE : packageFqNameSegments.get(0);
            shortNames.add(name);
        }
        return shortNames;
    }
}

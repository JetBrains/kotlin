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
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.di.InjectorForLazyResolveWithJava;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.List;

public enum AnalyzerFacadeForJVM implements AnalyzerFacade {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.io.*"),
            new ImportPath("kotlin.reflect.*")
    );

    public static class JvmSetup extends BasicSetup {

        private final JavaDescriptorResolver javaDescriptorResolver;

        public JvmSetup(@NotNull ResolveSession session, @NotNull JavaDescriptorResolver javaDescriptorResolver) {
            super(session);
            this.javaDescriptorResolver = javaDescriptorResolver;
        }

        @NotNull
        public JavaDescriptorResolver getJavaDescriptorResolver() {
            return javaDescriptorResolver;
        }
    }

    private AnalyzerFacadeForJVM() {
    }

    @NotNull
    @Override
    public JvmSetup createSetup(@NotNull Project fileProject, @NotNull Collection<JetFile> files) {
        return createSetup(fileProject, files, new BindingTraceContext(), true);
    }

    @NotNull
    public static ResolveSession createLazyResolveSession(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            boolean addBuiltIns
    ) {

        return createSetup(project, files, trace, addBuiltIns).getLazyResolveSession();
    }

    private static JvmSetup createSetup(
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            boolean addBuiltIns
    ) {
        GlobalContextImpl globalContext = ContextPackage.GlobalContext();

        DeclarationProviderFactory declarationProviderFactory =
                DeclarationProviderFactoryService.createDeclarationProviderFactory(project, globalContext.getStorageManager(), files);

        InjectorForLazyResolveWithJava resolveWithJava = new InjectorForLazyResolveWithJava(
                project,
                globalContext,
                declarationProviderFactory,
                trace);

        resolveWithJava.getModule().addFragmentProvider(
                DependencyKind.BINARIES, resolveWithJava.getJavaDescriptorResolver().getPackageFragmentProvider());

        if (addBuiltIns) {
            resolveWithJava.getModule().addFragmentProvider(
                    DependencyKind.BUILT_INS,
                    KotlinBuiltIns.getInstance().getBuiltInsModule().getPackageFragmentProvider());
        }
        return new JvmSetup(resolveWithJava.getResolveSession(), resolveWithJava.getJavaDescriptorResolver());
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            Predicate<PsiFile> filesToAnalyzeCompletely,
            ModuleDescriptorImpl module,
            MemberFilter memberFilter
    ) {
        GlobalContext globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                filesToAnalyzeCompletely,
                false,
                false
        );

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(project, topDownAnalysisParameters, trace, module,
                                                                                         memberFilter);
        try {
            module.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files);
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

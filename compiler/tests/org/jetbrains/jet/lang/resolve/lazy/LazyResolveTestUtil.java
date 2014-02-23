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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPackageDirective;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    public static InjectorForTopDownAnalyzer createInjectorForTDA(JetCoreEnvironment environment) {
        JetTestUtils.newTrace(environment);

        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                globalContext.getStorageManager(), globalContext.getExceptionTracker(), Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject());
        BindingTrace sharedTrace = support.getTrace();
        ModuleDescriptorImpl sharedModule = support.getModule();

        InjectorForTopDownAnalyzerForJvm injector =
                new InjectorForTopDownAnalyzerForJvm(environment.getProject(), params, sharedTrace, sharedModule);
        sharedModule.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());
        return injector;
    }

    public static ModuleDescriptor resolveEagerly(List<JetFile> files, JetCoreEnvironment environment) {
        JetTestUtils.newTrace(environment);

        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                globalContext.getStorageManager(), globalContext.getExceptionTracker(), Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject());
        BindingTrace sharedTrace = support.getTrace();
        ModuleDescriptorImpl sharedModule = support.getModule();

        InjectorForTopDownAnalyzerForJvm injector =
                new InjectorForTopDownAnalyzerForJvm(environment.getProject(), params, sharedTrace, sharedModule);
        sharedModule.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());
        injector.getTopDownAnalyzer().analyzeFiles(params, files, Collections.<AnalyzerScriptParameter>emptyList());
        return injector.getModuleDescriptor();
    }

    public static KotlinCodeAnalyzer resolveLazilyWithSession(List<JetFile> files, JetCoreEnvironment environment, boolean addBuiltIns) {
        JetTestUtils.newTrace(environment);

        Project project = environment.getProject();
        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(project);
        BindingTrace sharedTrace = support.getTrace();

        ResolveSession lazyResolveSession = AnalyzerFacadeForJVM.createLazyResolveSession(project, files, sharedTrace, addBuiltIns);
        support.setModule((ModuleDescriptorImpl)lazyResolveSession.getModuleDescriptor());

        return lazyResolveSession;
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
            JetPackageDirective directive = file.getPackageDirective();
            if (directive != null) {
                List<JetSimpleNameExpression> names = directive.getPackageNames();
                Name name = names.isEmpty() ? SpecialNames.ROOT_PACKAGE : names.get(0).getReferencedNameAsName();
                shortNames.add(name);
            }
            else {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }
        }
        return shortNames;
    }
}

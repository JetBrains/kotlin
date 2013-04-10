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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    private static ModuleDescriptor createModule(String name) {
        return new MutableModuleDescriptor(Name.special(name), JavaToKotlinClassMap.getInstance());
    }

    public static InjectorForTopDownAnalyzer getEagerInjectorForTopDownAnalyzer(TestCoreEnvironment environment) {
        ModuleDescriptor eagerModuleForLazy = createModule("<eager module for lazy>");

        InjectorForTopDownAnalyzer tdaInjectorForLazy = createInjectorForTDA(environment);
        // This line is required for the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetTestUtils.createFile(environment.getProject(), "empty.kt", "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    public static InjectorForTopDownAnalyzer createInjectorForTDA(TestCoreEnvironment environment) {
        environment.newTrace();

        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        Project project = environment.getProject();
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(project).getTrace();
        ModuleSourcesManager sourcesManager = KotlinModuleManager.SERVICE.getService(project).getSourcesManager();
        return new InjectorForTopDownAnalyzerForJvm(project, params, sharedTrace, sourcesManager);
    }

    public static SubModuleDescriptor resolveEagerly(TestCoreEnvironment environment) {
        InjectorForTopDownAnalyzer injector = createInjectorForTDA(environment);
        injector.getTopDownAnalyzer().analyzeFiles(environment.getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());
        return environment.getSubModuleDescriptor();
    }

    public static KotlinCodeAnalyzer resolveLazilyWithSession(TestCoreEnvironment environment) {
        environment.newTrace();

        final Project project = environment.getProject();
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).getTrace();

        final PsiClassFinder psiClassFinder = environment.getPsiClassFinder();

        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        final FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(
                storageManager,
                environment.getSourceFiles(),
                new Predicate<FqName>() {
                    @Override
                    public boolean apply(FqName fqName) {
                        return psiClassFinder.findPsiPackage(fqName) != null || new FqName("jet").equals(fqName);
                    }
                }
        );

        return new LazyCodeAnalyzer(
                project,
                storageManager,
                environment.getModuleSourcesManager(),
                environment.getSubModuleDescriptor(),
                declarationProviderFactory,
                Function.NULL,
                Predicates.<FqNameUnsafe>alwaysFalse(),
                sharedTrace);
    }

    public static SubModuleDescriptor resolveLazily(TestCoreEnvironment environment) {
        resolveLazilyWithSession(environment);
        return environment.getSubModuleDescriptor();
    }

    @NotNull
    public static Set<Name> getTopLevelPackagesFromFileList(@NotNull Collection<JetFile> files) {
        Set<Name> shortNames = Sets.newLinkedHashSet();
        for (JetFile file : files) {
            JetNamespaceHeader header = file.getNamespaceHeader();
            if (header != null) {
                List<JetSimpleNameExpression> names = header.getParentNamespaceNames();
                Name name = names.isEmpty() ? header.getNameAsName() : names.get(0).getReferencedNameAsName();
                shortNames.add(name);
            }
            else {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }
        }
        return shortNames;
    }
}

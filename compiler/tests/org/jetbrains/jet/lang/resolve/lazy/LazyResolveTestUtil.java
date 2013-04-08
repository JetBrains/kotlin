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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

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

    public static InjectorForTopDownAnalyzer getEagerInjectorForTopDownAnalyzer(JetCoreEnvironment environment) {
        ModuleDescriptor eagerModuleForLazy = createModule("<eager module for lazy>");

        InjectorForTopDownAnalyzer tdaInjectorForLazy = createInjectorForTDA(eagerModuleForLazy, environment);
        // This line is required for the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetTestUtils.createFile(environment.getProject(), "empty.kt", "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    public static InjectorForTopDownAnalyzer createInjectorForTDA(ModuleDescriptor module, JetCoreEnvironment environment) {
        JetTestUtils.newTrace(environment);

        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        Project project = environment.getProject();
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(project).getTrace();
        ModuleSourcesManager sourcesManager = KotlinModuleManager.SERVICE.getService(project).getSourcesManager();
        return new InjectorForTopDownAnalyzerForJvm(project, params, sharedTrace, sourcesManager);
    }

    public static ModuleDescriptor resolveEagerly(Collection<JetFile> files, JetCoreEnvironment environment) {
        ModuleDescriptor module = createModule("<test module>");
        InjectorForTopDownAnalyzer injector = createInjectorForTDA(module, environment);
        injector.getTopDownAnalyzer().analyzeFiles(files, Collections.<AnalyzerScriptParameter>emptyList());
        return module;
    }

    public static KotlinCodeAnalyzer resolveLazilyWithSession(Collection<JetFile> files, JetCoreEnvironment environment) {
        JetTestUtils.newTrace(environment);

        ModuleDescriptor javaModule = createModule("<java module>");

        final Project project = environment.getProject();
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).getTrace();
        InjectorForJavaDescriptorResolver injector =
                new InjectorForJavaDescriptorResolver(
                        project,
                        sharedTrace,
                        null, // TODO resolutionFacade,
                        new LockBasedStorageManager(),
                        null, // TODO subModule,
                        null //TODO searchScope
                );
        final PsiClassFinder psiClassFinder = injector.getPsiClassFinder();
        final JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();


        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        final FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(storageManager, files, new Predicate<FqName>() {
            @Override
            public boolean apply(FqName fqName) {
                return psiClassFinder.findPsiPackage(fqName) != null || new FqName("jet").equals(fqName);
            }
        });

        ModuleConfiguration moduleConfiguration = new ModuleConfiguration() {
            @Override
            public List<ImportPath> getDefaultImports() {
                List<ImportPath> imports = Lists.newArrayList(new ImportPath("java.lang.*"));
                imports.addAll(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);
                return imports;
            }

            @Override
            public void extendNamespaceScope(
                    @NotNull BindingTrace trace,
                    @NotNull PackageViewDescriptor namespaceDescriptor,
                    @NotNull WritableScope namespaceMemberScope
            ) {
                FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
                if (new FqName("jet").equals(fqName)) {
                    namespaceMemberScope.importScope(KotlinBuiltIns.getInstance().getBuiltInsScope());
                }
                if (psiClassFinder.findPsiPackage(fqName) != null) {
                    JetScope javaPackageScope = javaDescriptorResolver.getJavaPackageScope(namespaceDescriptor);
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

        ModuleDescriptor lazyModule = createModule("<lazy module>");
        return new ResolveSession(project, storageManager, lazyModule, moduleConfiguration, declarationProviderFactory, sharedTrace);
    }

    public static ModuleDescriptor resolveLazily(Collection<JetFile> files, JetCoreEnvironment environment) {
        resolveLazilyWithSession(files, environment);
        return KotlinModuleManager.SERVICE.getService(environment.getProject()).getModules().iterator().next();
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

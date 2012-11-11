/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    public static InjectorForTopDownAnalyzer getEagerInjectorForTopDownAnalyzer(JetCoreEnvironment environment) {
        ModuleDescriptor eagerModuleForLazy = new ModuleDescriptor(Name.special("<eager module for lazy>"));

        InjectorForTopDownAnalyzer tdaInjectorForLazy = createInjectorForTDA(eagerModuleForLazy, environment);
        // This line is required fro the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetPsiFactory.createFile(environment.getProject(), "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    public static InjectorForTopDownAnalyzer createInjectorForTDA(ModuleDescriptor module, JetCoreEnvironment environment) {
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        return new InjectorForTopDownAnalyzerForJvm(environment.getProject(), params, new BindingTraceContext(), module);
    }

    public static ModuleDescriptor resolveEagerly(List<JetFile> files, JetCoreEnvironment environment) {
        ModuleDescriptor module = new ModuleDescriptor(Name.special("<test module>"));
        InjectorForTopDownAnalyzer injector = createInjectorForTDA(module, environment);
        injector.getTopDownAnalyzer().analyzeFiles(files, Collections.<AnalyzerScriptParameter>emptyList());
        return module;
    }

    public static ModuleDescriptor resolveLazily(List<JetFile> files, JetCoreEnvironment environment) {
        ModuleDescriptor javaModule = new ModuleDescriptor(Name.special("<java module>"));

        final Project project = environment.getProject();
        InjectorForJavaDescriptorResolver injector =
                new InjectorForJavaDescriptorResolver(project, new BindingTraceContext(), javaModule);
        final PsiClassFinder psiClassFinder = injector.getPsiClassFinder();
        final JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();


        final FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(files, new Predicate<FqName>() {
            @Override
            public boolean apply(FqName fqName) {
                return psiClassFinder.findPsiPackage(fqName) != null || new FqName("jet").equals(fqName);
            }
        });

        ModuleConfiguration moduleConfiguration = new ModuleConfiguration() {
            @Override
            public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
                directives.add(JetPsiFactory.createImportDirective(project, new ImportPath("java.lang.*")));
                for (ImportPath defaultJetImport : DefaultModuleConfiguration.DEFAULT_JET_IMPORTS) {
                    directives.add(JetPsiFactory.createImportDirective(project, defaultJetImport));
                }
            }

            @Override
            public void extendNamespaceScope(
                    @NotNull BindingTrace trace,
                    @NotNull NamespaceDescriptor namespaceDescriptor,
                    @NotNull WritableScope namespaceMemberScope
            ) {
                FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
                if (new FqName("jet").equals(fqName)) {
                    namespaceMemberScope.importScope(KotlinBuiltIns.getInstance().getBuiltInsScope());
                }
                if (psiClassFinder.findPsiPackage(fqName) != null) {
                    JavaPackageScope javaPackageScope = javaDescriptorResolver.getJavaPackageScope(namespaceDescriptor);
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

        ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));
        ResolveSession
                session = new ResolveSession(project, lazyModule, moduleConfiguration, declarationProviderFactory);
        return lazyModule;
    }

    @NotNull
    public static Set<Name> getTopLevelPackagesFromFileList(@NotNull List<JetFile> files) {
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

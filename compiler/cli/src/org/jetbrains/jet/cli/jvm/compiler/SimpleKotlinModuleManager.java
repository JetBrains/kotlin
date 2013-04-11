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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.TraceBasedLightClassResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaClassResolutionFacadeImpl;
import org.jetbrains.jet.lang.resolve.java.JavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * KotlinModuleManager that assumes that all the sources belong to the same submodule
 */
public class SimpleKotlinModuleManager implements KotlinModuleManager {

    private final NotNullLazyValue<Modules> modules;

    public SimpleKotlinModuleManager(
            @NotNull final JetCoreEnvironment jetCoreEnvironment,
            @NotNull final String baseName,
            @NotNull final PlatformToKotlinClassMap classMap
    ) {
        this.modules = new NotNullLazyValue<Modules>() {
            @NotNull
            @Override
            protected Modules compute() {
                return createModules(jetCoreEnvironment, baseName, classMap);
            }
        };
    }

    public static Modules createModules(
            @NotNull JetCoreEnvironment jetCoreEnvironment,
            @NotNull String baseName,
            @NotNull PlatformToKotlinClassMap classMap
    ) {
        Project project = jetCoreEnvironment.getProject();
        MutableModuleSourcesManager sourcesManager = createModuleSourcesManager(project);
        MutableModuleDescriptor module = new MutableModuleDescriptor(Name.special("<" + baseName + " module>"), classMap);
        MutableSubModuleDescriptor subModule = new MutableSubModuleDescriptor(module, Name.special("<" + baseName + " sub-module>"));
        module.addSubModule(subModule);
        subModule.addDependency(KotlinBuiltIns.getInstance().getBuiltInsSubModule());
        subModule.addDependency(SubModuleDescriptor.MY_SOURCE);

        for (JetFile file : jetCoreEnvironment.getSourceFiles()) {
            sourcesManager.registerRoot(subModule, PackageFragmentKind.SOURCE, file.getVirtualFile());
        }

        addDefaultImports(subModule, JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS);
        addDefaultImports(subModule, DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);

        BindingTrace trace = CliLightClassGenerationSupport.getInstanceForCli(project).getTrace();

        StorageManager storageManager = new LockBasedStorageManager();
        JavaClassResolutionFacadeImpl classResolutionFacade = new JavaClassResolutionFacadeImpl(
                new TraceBasedLightClassResolver(trace.getBindingContext()));
        InjectorForJavaDescriptorResolver drInjector = new InjectorForJavaDescriptorResolver(
                project, trace, classResolutionFacade, storageManager, subModule, GlobalSearchScope.allScope(project)
        );

        JavaPackageFragmentProvider javaPackageFragmentProvider = drInjector.getJavaPackageFragmentProvider();

        subModule.addPackageFragmentProvider(javaPackageFragmentProvider);
        classResolutionFacade.addPackageFragmentProvider(javaPackageFragmentProvider);

        return new Modules(module, sourcesManager, drInjector);
    }

    private static MutableModuleSourcesManager createModuleSourcesManager(@NotNull Project project) {
        return ApplicationManager.getApplication().isUnitTestMode()
                    ? new MutableModuleSourcesManagerForTests(project)
                    : new MutableModuleSourcesManager(project);
    }

    private static void addDefaultImports(MutableSubModuleDescriptor subModule, List<ImportPath> imports) {
        for (ImportPath path : imports) {
            subModule.addDefaultImport(path);
        }
    }

    @NotNull
    @Override
    public Collection<ModuleDescriptor> getModules() {
        return Collections.singletonList(modules.getValue().module);
    }

    @NotNull
    @Override
    public ModuleSourcesManager getSourcesManager() {
        return modules.getValue().moduleSourcesManager;
    }

    @NotNull
    public InjectorForJavaDescriptorResolver getInjectorForJavaDescriptorResolver() {
        return modules.getValue().drInjector;
    }

    private static class Modules {
        private final ModuleDescriptor module;
        private final ModuleSourcesManager moduleSourcesManager;
        private final InjectorForJavaDescriptorResolver drInjector;

        private Modules(ModuleDescriptor module, ModuleSourcesManager moduleSourcesManager, InjectorForJavaDescriptorResolver drInjector) {
            this.module = module;
            this.moduleSourcesManager = moduleSourcesManager;
            this.drInjector = drInjector;
        }
    }
}

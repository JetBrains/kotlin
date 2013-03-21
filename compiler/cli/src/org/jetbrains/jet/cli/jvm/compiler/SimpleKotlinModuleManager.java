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

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.KotlinModuleManager;
import org.jetbrains.jet.lang.resolve.ModuleSourcesManager;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
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
        MutableModuleDescriptor mutableModule = new MutableModuleDescriptor(Name.special("<" + baseName + " module>"), classMap);
        final MutableSubModuleDescriptor subModule = new MutableSubModuleDescriptor(mutableModule, Name.special("<" + baseName + " sub-module>"));
        mutableModule.addSubModule(subModule);

        subModule.addDependency(KotlinBuiltIns.getInstance().getBuiltInsSubModule());

        addDefaultImports(subModule, JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS);
        addDefaultImports(subModule, DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);

        final CliIndexManager index = CliIndexManager.getInstance(jetCoreEnvironment.getProject());
        ModuleSourcesManager moduleSourcesManager = new ModuleSourcesManager() {

            @NotNull
            @Override
            public SubModuleDescriptor getSubModuleForFile(@NotNull PsiFile file) {
                return subModule;
            }

            @NotNull
            @Override
            public Collection<JetFile> getPackageFragmentSources(@NotNull PackageFragmentDescriptor packageFragment) {
                return index.getPackageSources(packageFragment.getFqName());
            }
        };

        return new Modules(mutableModule, moduleSourcesManager);
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

    private static class Modules {
        private final ModuleDescriptor module;
        private final ModuleSourcesManager moduleSourcesManager;

        private Modules(ModuleDescriptor module, ModuleSourcesManager moduleSourcesManager) {
            this.module = module;
            this.moduleSourcesManager = moduleSourcesManager;
        }

    }
}

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

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.KotlinModuleManager;
import org.jetbrains.jet.lang.resolve.ModuleSourcesManager;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;

/**
 * KotlinModuleManager that assumes that all the sources belong to the same submodule
 */
public class SimpleKotlinModuleManager implements KotlinModuleManager {

    private final ModuleDescriptor module;
    private final ModuleSourcesManager moduleSourcesManager;

    public SimpleKotlinModuleManager(
            @NotNull JetCoreEnvironment jetCoreEnvironment,
            @NotNull String baseName,
            @NotNull PlatformToKotlinClassMap classMap
    ) {
        MutableModuleDescriptor mutableModule = new MutableModuleDescriptor(Name.special("<" + baseName + " module>"), classMap);
        final SubModuleDescriptor subModule =
                mutableModule.addSubModule(new MutableSubModuleDescriptor(mutableModule, Name.special("<" + baseName + " sub-module>")));
        final CliIndexManager index = CliIndexManager.getInstance(jetCoreEnvironment.getProject());
        this.moduleSourcesManager = new ModuleSourcesManager() {

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
        this.module = mutableModule;
    }

    @NotNull
    @Override
    public Collection<ModuleDescriptor> getModules() {
        return Collections.singletonList(module);
    }

    @NotNull
    @Override
    public ModuleSourcesManager getSourcesManager() {
        return moduleSourcesManager;
    }
}

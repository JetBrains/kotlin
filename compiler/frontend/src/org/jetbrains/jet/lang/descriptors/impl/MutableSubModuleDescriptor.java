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

package org.jetbrains.jet.lang.descriptors.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MutableSubModuleDescriptor extends AbstractSubModuleDescriptor {

    private final MutablePackageFragmentProvider packageFragmentProviderForKotlinSources;

    private final List<PackageFragmentProvider> providers = Lists.newArrayList();
    private final CompositePackageFragmentProvider compositePackageFragmentProvider = new CompositePackageFragmentProvider(providers);

    private final Set<SubModuleDescriptor> dependencies = Sets.newLinkedHashSet();
    private final List<ImportPath> defaultImports = Lists.newArrayList();

    public MutableSubModuleDescriptor(@NotNull ModuleDescriptor module, @NotNull Name name) {
        super(module, name);
        this.packageFragmentProviderForKotlinSources = new MutablePackageFragmentProvider(this);
        providers.add(packageFragmentProviderForKotlinSources);
    }

    @NotNull
    @Override
    public PackageFragmentProvider getPackageFragmentProvider() {
        return compositePackageFragmentProvider;
    }

    public void addPackageFragmentProvider(@NotNull PackageFragmentProvider provider) {
        providers.add(provider);
    }

    @NotNull
    public MutablePackageFragmentProvider getPackageFragmentProviderForKotlinSources() {
        return packageFragmentProviderForKotlinSources;
    }

    @NotNull
    @Override
    public Collection<SubModuleDescriptor> getDependencies() {
        return dependencies;
    }

    public void addDependency(@NotNull SubModuleDescriptor dependency) {
        dependencies.add(dependency);
    }

    @NotNull
    @Override
    public List<ImportPath> getDefaultImports() {
        return Collections.unmodifiableList(defaultImports);
    }

    public void addDefaultImport(@NotNull ImportPath importPath) {
        defaultImports.add(importPath);
    }
}

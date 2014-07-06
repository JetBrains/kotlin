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
import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleDescriptorImpl extends DeclarationDescriptorImpl implements ModuleDescriptor {
    private static final Logger LOG = Logger.getInstance(ModuleDescriptorImpl.class);

    private final Map<DependencyKind, List<PackageFragmentProvider>> prioritizedFragmentProviders = Maps.newHashMap();
    private final List<PackageFragmentProvider> fragmentProviders = Lists.newArrayList();
    private final CompositePackageFragmentProvider packageFragmentProvider = new CompositePackageFragmentProvider(fragmentProviders);
    private final List<ImportPath> defaultImports;
    private final PlatformToKotlinClassMap platformToKotlinClassMap;

    public ModuleDescriptorImpl(
            @NotNull Name name,
            @NotNull List<ImportPath> defaultImports,
            @NotNull PlatformToKotlinClassMap platformToKotlinClassMap
    ) {
        super(Annotations.EMPTY, name);
        if (!name.isSpecial()) {
            throw new IllegalArgumentException("module name must be special: " + name);
        }
        this.defaultImports = defaultImports;
        this.platformToKotlinClassMap = platformToKotlinClassMap;
    }

    public void addFragmentProvider(@NotNull DependencyKind dependencyKind, @NotNull PackageFragmentProvider provider) {
        if (fragmentProviders.contains(provider)) {
            LOG.error("Trying to add already present fragment provider: " + provider);
        }
        List<PackageFragmentProvider> providers = prioritizedFragmentProviders.get(dependencyKind);
        if (providers == null) {
            providers = new ArrayList<PackageFragmentProvider>(1);
            prioritizedFragmentProviders.put(dependencyKind, providers);
        }
        providers.add(provider);
        buildProvidersList();
    }

    private void buildProvidersList() {
        fragmentProviders.clear();
        for (DependencyKind dependencyKind : DependencyKind.values()) {
            List<PackageFragmentProvider> providers = prioritizedFragmentProviders.get(dependencyKind);
            if (providers != null) {
                fragmentProviders.addAll(providers);
            }
        }
    }

    @Override
    @Nullable
    public DeclarationDescriptor getContainingDeclaration() {
        return null;
    }

    @NotNull
    @Override
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull FqName fqName) {
        List<PackageFragmentDescriptor> fragments = packageFragmentProvider.getPackageFragments(fqName);
        return !fragments.isEmpty() ? new PackageViewDescriptorImpl(this, fqName, fragments) : null;
    }

    @NotNull
    @Override
    public List<ImportPath> getDefaultImports() {
        return defaultImports;
    }

    @NotNull
    @Override
    public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
        return platformToKotlinClassMap;
    }

    @NotNull
    @Override
    public ModuleDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDeclaration(this, data);
    }
}

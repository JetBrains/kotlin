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

package org.jetbrains.jet.lang.resolve.lazy.declarations;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class SubModuleDefinitionImpl implements SubModuleDefinition {

    private final Name name;
    private final Collection<SubModuleDefinition> dependenciesToResolve = Lists.newArrayList();
    private final Collection<SubModuleDescriptor> externalDependencies = Lists.newArrayList();

    public SubModuleDefinitionImpl(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @NotNull
    @Override
    public Collection<SubModuleDefinition> getDependenciesToBeResolved() {
        return Collections.unmodifiableCollection(dependenciesToResolve);
    }

    public void addDependencyToBeResolved(@NotNull SubModuleDefinition definition) {
        dependenciesToResolve.add(definition);
    }

    @NotNull
    @Override
    public Collection<SubModuleDescriptor> getExternalDependencies() {
        return Collections.unmodifiableCollection(externalDependencies);
    }

    public void addExternalDependency(@NotNull SubModuleDescriptor subModule) {
        externalDependencies.add(subModule);
    }

    @NotNull
    @Override
    public abstract List<PackageFragmentDescriptor> getPackageFragmentsLoadedExternally(
            @NotNull FqName fqName, @NotNull SubModuleDescriptor targetSubModule
    );

    @Nullable
    @Override
    public abstract PackageMemberDeclarationProvider getPackageMembersFromSourceFiles(@NotNull FqName fqName);
}

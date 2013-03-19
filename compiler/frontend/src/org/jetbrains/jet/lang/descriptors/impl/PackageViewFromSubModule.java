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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.List;

public class PackageViewFromSubModule extends PackageViewDescriptorImpl {

    public PackageViewFromSubModule(
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName
    ) {
        this(subModule, fqName, DescriptorUtils.getPackageFragmentsIncludingDependencies(subModule, fqName));
    }

    public PackageViewFromSubModule(
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName,
            @NotNull List<PackageFragmentDescriptor> fragments
    ) {
        super(subModule, fqName, fragments);
    }

    @NotNull
    @Override
    protected Collection<PackageViewDescriptor> getSubPackages(@NotNull FqName fqName) {
        Collection<FqName> subPackages = getViewContext().getPackageFragmentProvider().getSubPackagesOf(fqName);
        return Collections2.transform(subPackages,
                                      new Function<FqName, PackageViewDescriptor>() {
                                          @Override
                                          public PackageViewDescriptor apply(FqName subPackage) {
                                              return getViewContext().getPackageView(subPackage);
                                          }
                                      });
    }

    @NotNull
    @Override
    public SubModuleDescriptor getViewContext() {
        return (SubModuleDescriptor) super.getViewContext();
    }

    @Nullable
    @Override
    public PackageViewDescriptor getContainingDeclaration() {
        if (getFqName().isRoot()) return null;
        return getViewContext().getPackageView(getFqName().parent());
    }

    @Nullable
    @Override
    protected PackageViewDescriptor getSubPackage(@NotNull Name name) {
        return getViewContext().getPackageView(getFqName().child(name));
    }
}

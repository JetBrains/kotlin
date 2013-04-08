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

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;

public class PackageViewFromSubModule extends PackageLikeDescriptorBase implements PackageViewDescriptor {

    private final SubModuleDescriptor subModule;
    private final JetScope scope;

    public PackageViewFromSubModule(
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName,
            @NotNull final List<PackageFragmentDescriptor> fragments
    ) {
        super(fqName);
        this.subModule = subModule;
        List<JetScope> fragmentScopes = ContainerUtil.mapNotNull(
                fragments,
                new Function<PackageFragmentDescriptor, JetScope>() {
                    @Override
                    public JetScope fun(PackageFragmentDescriptor input) {
                        return input.getMemberScope();
                    }
                });
        this.scope = new ChainedScope(this, "Joint scope for " + this, fragmentScopes) {
            @Override
            public PackageViewDescriptor getPackage(@NotNull Name name) {
                return getSubPackage(name);
            }

            @NotNull
            @Override
            public Collection<DeclarationDescriptor> getAllDescriptors() {
                Collection<DeclarationDescriptor> descriptors = Lists.newArrayList(super.getAllDescriptors());
                for (PackageFragmentDescriptor fragment : fragments) {
                    descriptors.addAll(getSubPackages(fragment.getFqName()));
                }
                return descriptors;
            }
        };
    }

    @NotNull
    private Collection<PackageViewDescriptor> getSubPackages(@NotNull FqName fqName) {
        Collection<FqName> subPackages = getViewContext().getPackageFragmentProvider().getSubPackagesOf(fqName);
        return Collections2.transform(subPackages,
                                      new com.google.common.base.Function<FqName, PackageViewDescriptor>() {
                                          @Override
                                          public PackageViewDescriptor apply(FqName subPackage) {
                                              return getViewContext().getPackageView(subPackage);
                                          }
                                      });
    }

    @NotNull
    @Override
    public SubModuleDescriptor getViewContext() {
        return subModule;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getContainingDeclaration() {
        if (getFqName().isRoot()) return null;
        return getViewContext().getPackageView(getFqName().parent());
    }

    @Nullable
    private PackageViewDescriptor getSubPackage(@NotNull Name name) {
        return getViewContext().getPackageView(getFqName().child(name));
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return scope;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageViewDescriptor(this, data);
    }
}

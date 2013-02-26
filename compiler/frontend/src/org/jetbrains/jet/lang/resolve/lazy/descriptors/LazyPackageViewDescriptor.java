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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PackageViewFromSubModule;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.List;

public class LazyPackageViewDescriptor extends PackageViewFromSubModule implements LazyDescriptor {
    private final List<PackageFragmentDescriptor> ownFragments;

    public LazyPackageViewDescriptor(
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName,
            @NotNull List<PackageFragmentDescriptor> ownFragments,
            @NotNull List<PackageFragmentDescriptor> fragmentsFromDependencies
    ) {
        super(subModule, fqName, ContainerUtil.concat(ownFragments, fragmentsFromDependencies));
        this.ownFragments = ownFragments;
    }

    @Override
    public void forceResolveAllContents() {
        for (PackageFragmentDescriptor fragment : ownFragments) {
            ForceResolveUtil.forceResolveAllContents(fragment);
        }
    }
}

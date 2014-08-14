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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.*;

public class CompositePackageFragmentProvider implements PackageFragmentProvider {
    // can be modified from outside
    private final List<PackageFragmentProvider> providers;

    public CompositePackageFragmentProvider(@NotNull List<PackageFragmentProvider> providers) {
        this.providers = providers;
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        ArrayList<PackageFragmentDescriptor> result = new ArrayList<PackageFragmentDescriptor>();
        for (PackageFragmentProvider provider : providers) {
            result.addAll(provider.getPackageFragments(fqName));
        }
        result.trimToSize();
        return result;
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        Set<FqName> result = new HashSet<FqName>();
        for (PackageFragmentProvider provider : providers) {
            result.addAll(provider.getSubPackagesOf(fqName));
        }
        return result;
    }
}

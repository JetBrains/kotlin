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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.List;

public class MutablePackageFragmentProvider implements PackageFragmentProvider {
    private final Multimap<FqName, MutablePackageFragmentDescriptor> packageFragments = ArrayListMultimap.create();

    private final SubModuleDescriptor subModule;

    public MutablePackageFragmentProvider(@NotNull SubModuleDescriptor subModule) {
        this.subModule = subModule;
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        //noinspection unchecked
        return (List) packageFragments.get(fqName);
    }

    @NotNull
    public MutablePackageFragmentDescriptor addPackageFragment(@NotNull PackageFragmentKind kind, @NotNull FqName fqName) {
        // We use one fragment per kind
        Collection<MutablePackageFragmentDescriptor> fragments = packageFragments.get(fqName);
        for (MutablePackageFragmentDescriptor fragment : fragments) {
            if (kind.equals(fragment.getKind())) return fragment;
        }

        MutablePackageFragmentDescriptor result = new MutablePackageFragmentDescriptor(subModule, kind, fqName);
        packageFragments.put(fqName, result);

        return result;
    }

}

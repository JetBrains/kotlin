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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PackageIndex {

    @NotNull
    Set<FqName> getAllPackages();

    @NotNull
    Set<FqName> getSubPackagesOf(@NotNull FqName parent);

    class Builder implements PackageIndex {
        private static final Name PRESENCE_MARKER = Name.special("<no children>");

        // foo -> {foo, foo.bar}
        private final SetMultimap<FqName, Name> subPackages = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        private final PackageIndex delegate = new PackageIndexImpl(subPackages.asMap());

        public void addPackage(@NotNull FqName fqName) {
            subPackages.put(fqName, PRESENCE_MARKER);
            if (fqName.isRoot()) return;
            subPackages.put(fqName.parent(), fqName.shortName());
            addPackage(fqName.parent());
        }

        @NotNull
        public PackageIndex build() {
            return new PackageIndexImpl(ImmutableMap.copyOf(subPackages.asMap()));
        }

        @NotNull
        @Override
        public Set<FqName> getAllPackages() {
            return delegate.getAllPackages();
        }

        @NotNull
        @Override
        public Set<FqName> getSubPackagesOf(@NotNull FqName parent) {
            return delegate.getSubPackagesOf(parent);
        }

        private static class PackageIndexImpl implements  PackageIndex {

            // foo -> {foo, foo.bar}
            private final Map<FqName, Collection<Name>> subPackages;

            public PackageIndexImpl(Map<FqName, Collection<Name>> subPackages) {
                this.subPackages = subPackages;
            }

            @Override
            @NotNull
            public Set<FqName> getAllPackages() {
                return subPackages.keySet();
            }

            @Override
            @NotNull
            public Set<FqName> getSubPackagesOf(@NotNull FqName parent) {
                return doGetSubPackagesOf(parent, Sets.<FqName>newHashSet());
            }

            private Set<FqName> doGetSubPackagesOf(FqName parent, Set<FqName> result) {
                Collection<Name> immediateChildren = subPackages.get(parent);
                if (immediateChildren == null) {
                    return result;
                }
                for (Name childName : immediateChildren) {
                    if (childName == PRESENCE_MARKER) continue;
                    result.add(parent.child(childName));
                }
                return result;
            }

        }
    }


}

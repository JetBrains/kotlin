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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PackageIndex {

    public static final Name PRESENCE_MARKER = Name.special("<no children>");

    public static class Builder {
        // foo -> {foo, foo.bar}
        private final Multimap<FqName, Name> subPackages = HashMultimap.create();

        public void addPackage(@NotNull FqName fqName) {
            subPackages.put(fqName, PRESENCE_MARKER);
            if (fqName.isRoot()) return;
            subPackages.put(fqName.parent(), fqName.shortName());
            addPackage(fqName.parent());
        }

        @NotNull
        public PackageIndex build() {
            return new PackageIndex(subPackages.asMap());
        }
    }

    // foo -> {foo, foo.bar}
    private final ImmutableMap<FqName, Collection<Name>> subPackages;

    public PackageIndex(Map<FqName, Collection<Name>> subPackages) {
        this.subPackages = ImmutableMap.copyOf(subPackages);
    }

    @NotNull
    public Set<FqName> getAllPackages() {
        return subPackages.keySet();
    }

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

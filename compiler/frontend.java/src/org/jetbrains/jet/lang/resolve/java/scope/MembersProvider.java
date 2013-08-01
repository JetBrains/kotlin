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

package org.jetbrains.jet.lang.resolve.java.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

/* package */ class MembersProvider {
    @Nullable
    private final JavaClass javaClass;
    @Nullable
    private final JavaPackage javaPackage;
    private final boolean staticMembers;

    private MembersProvider(@Nullable JavaClass javaClass, @Nullable JavaPackage javaPackage, boolean staticMembers) {
        this.javaClass = javaClass;
        this.javaPackage = javaPackage;
        this.staticMembers = staticMembers;
    }

    @NotNull
    public static MembersProvider forPackage(@NotNull JavaPackage javaPackage) {
        return new MembersProvider(null, javaPackage, true);
    }

    @NotNull
    public static MembersProvider forClass(@NotNull JavaClass javaClass, boolean staticMembers) {
        return new MembersProvider(javaClass, null, staticMembers);
    }

    @Nullable
    public NamedMembers get(@NotNull Name name) {
        return getMembersCache().get(name);
    }

    @NotNull
    public Collection<NamedMembers> allMembers() {
        return getMembersCache().allMembers();
    }

    private MembersCache membersCache;
    @NotNull
    private MembersCache getMembersCache() {
        if (membersCache == null) {
            membersCache = MembersCache.buildMembersByNameCache(javaClass, javaPackage, staticMembers);
        }
        return membersCache;
    }
}

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

import jet.Function0;
import jet.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaMemberResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaEnumClassObjectScope extends JavaClassMembersScope {
    public JavaEnumClassObjectScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull JavaClass javaClass,
            @NotNull JavaMemberResolver memberResolver
    ) {
        super(descriptor, MembersProvider.forClass(javaClass, true), memberResolver);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>();

        final Collection<NamedMembers> enumNonStaticMembers = membersProvider.allMembers();

        NotNullLazyValue<Collection<Name>> enumMemberNames =
                LockBasedStorageManager.NO_LOCKS.createLazyValue(new Function0<Collection<Name>>() {
                    @Override
                    public Collection<Name> invoke() {
                        return KotlinPackage.map(enumNonStaticMembers, new Function1<NamedMembers, Name>() {
                            @Override
                            public Name invoke(@NotNull NamedMembers members) {
                                return members.getName();
                            }
                        });
                    }
                });

        for (NamedMembers members : enumNonStaticMembers) {
            for (JavaField field : members.getFields()) {
                if (field.isEnumEntry()) {
                    EnumEntrySyntheticClassDescriptor enumEntry = EnumEntrySyntheticClassDescriptor
                            .create(LockBasedStorageManager.NO_LOCKS, (ClassDescriptor) descriptor, members.getName(), enumMemberNames);
                    result.add(enumEntry);
                    break;
                }
            }
        }

        return result;
    }
}

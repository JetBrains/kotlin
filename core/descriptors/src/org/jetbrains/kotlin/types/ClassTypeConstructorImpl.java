/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassTypeConstructorImpl extends AbstractClassTypeConstructor implements TypeConstructor {
    private final ClassDescriptor classDescriptor;
    private final List<TypeParameterDescriptor> parameters;
    private final Collection<KotlinType> supertypes;
    private final boolean isFinal;

    public ClassTypeConstructorImpl(
            @NotNull ClassDescriptor classDescriptor,
            boolean isFinal,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<KotlinType> supertypes
    ) {
        super(LockBasedStorageManager.NO_LOCKS);
        this.classDescriptor = classDescriptor;
        this.isFinal = isFinal;
        this.parameters = Collections.unmodifiableList(new ArrayList<TypeParameterDescriptor>(parameters));
        this.supertypes = Collections.unmodifiableCollection(supertypes);
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return DescriptorUtils.getFqName(classDescriptor).asString();
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean isDenotable() {
        return true;
    }

    @Override
    @NotNull
    public ClassDescriptor getDeclarationDescriptor() {
        return classDescriptor;
    }

    @NotNull
    @Override
    protected Collection<KotlinType> computeSupertypes() {
        return supertypes;
    }

    @NotNull
    @Override
    protected SupertypeLoopChecker getSupertypeLoopChecker() {
        return SupertypeLoopChecker.EMPTY.INSTANCE;
    }
}

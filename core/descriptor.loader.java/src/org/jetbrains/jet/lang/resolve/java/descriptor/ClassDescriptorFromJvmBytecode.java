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

package org.jetbrains.jet.lang.resolve.java.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassNonStaticMembersScope;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @see org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor
 */
public class ClassDescriptorFromJvmBytecode extends MutableClassDescriptorLite implements JavaClassDescriptor {
    private JetType functionTypeForSamInterface;
    private JavaClassNonStaticMembersScope scopeForConstructorResolve;
    private ConstructorDescriptor primaryConstructor;
    private Collection<ConstructorDescriptor> constructors;

    public ClassDescriptorFromJvmBytecode(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull ClassKind kind,
            boolean isInner
    ) {
        super(containingDeclaration, name, kind, isInner);
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        assert scopeForConstructorResolve != null;
        if (constructors == null) {
            constructors = scopeForConstructorResolve.getConstructors();
        }
        return constructors;
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        if (primaryConstructor == null) {
            for (ConstructorDescriptor constructor : getConstructors()) {
                if (constructor.isPrimary()) {
                    if (primaryConstructor != null) {
                        throw new IllegalStateException(
                                "Class has more than one primary constructor: " + primaryConstructor + "\n" + constructor);
                    }
                    primaryConstructor = constructor;
                }
            }
        }
        return primaryConstructor;
    }

    public void setScopeForConstructorResolve(@NotNull JavaClassNonStaticMembersScope scopeForConstructorResolve) {
        this.scopeForConstructorResolve = scopeForConstructorResolve;
    }

    @Override
    @Nullable
    public JetType getFunctionTypeForSamInterface() {
        return functionTypeForSamInterface;
    }

    public void setFunctionTypeForSamInterface(@NotNull JetType functionTypeForSamInterface) {
        this.functionTypeForSamInterface = functionTypeForSamInterface;
    }
}

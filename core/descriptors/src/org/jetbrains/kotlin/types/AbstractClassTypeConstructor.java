/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    private boolean hashCodeComputed;
    private int hashCode;

    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @Override
    public final int hashCode() {
        if (!hashCodeComputed) {
            hashCodeComputed = true;
            ClassifierDescriptor descriptor = getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && hasMeaningfulFqName(descriptor)) {
                hashCode = DescriptorUtils.getFqName(descriptor).hashCode();
            }
            else {
                hashCode = System.identityHashCode(this);
            }
        }
        return hashCode;
    }

    @NotNull
    @Override
    public abstract ClassifierDescriptor getDeclarationDescriptor();

    @NotNull
    @Override
    public KotlinBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TypeConstructor)) return false;

        // performance optimization: getFqName is slow method
        if (other.hashCode() != hashCode()) return false;

        ClassifierDescriptor myDescriptor = getDeclarationDescriptor();
        ClassifierDescriptor otherDescriptor = ((TypeConstructor) other).getDeclarationDescriptor();

        // descriptor for type is created once per module
        if (myDescriptor == otherDescriptor) return true;

        // All error types have the same descriptor
        if (!hasMeaningfulFqName(myDescriptor) ||
            otherDescriptor != null && !hasMeaningfulFqName(otherDescriptor)) {
            return this == other;
        }

        if (myDescriptor instanceof ClassDescriptor && otherDescriptor instanceof ClassDescriptor) {
            FqNameUnsafe otherFqName = DescriptorUtils.getFqName(otherDescriptor);
            FqNameUnsafe myFqName = DescriptorUtils.getFqName(myDescriptor);
            return myFqName.equals(otherFqName);
        }

        return false;
    }

    private static boolean hasMeaningfulFqName(@NotNull ClassifierDescriptor descriptor) {
        return !ErrorUtils.isError(descriptor) &&
               !DescriptorUtils.isLocal(descriptor);
    }

    @NotNull
    @Override
    protected Collection<KotlinType> getAdditionalNeighboursInSupertypeGraph() {
        // We suppose that there is an edge from C to A in graph when disconnecting loops in supertypes,
        // because such cyclic declarations should be prohibited (see p.10.2.1 of Kotlin spec)
        // class A : B {
        //   static class C {}
        // }
        // class B : A.C {}
        DeclarationDescriptor containingDeclaration = getDeclarationDescriptor().getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            return Collections.<KotlinType>singleton(((ClassDescriptor) containingDeclaration).getDefaultType());
        }
        return Collections.emptyList();
    }
}

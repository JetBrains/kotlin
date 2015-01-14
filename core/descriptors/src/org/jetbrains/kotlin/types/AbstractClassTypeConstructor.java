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
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

public abstract class AbstractClassTypeConstructor implements TypeConstructor {
    @Override
    public final int hashCode() {
        return hashCode(this);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return equals(this, obj);
    }

    public static boolean equals(@NotNull TypeConstructor me, Object other) {
        if (!(other instanceof TypeConstructor)) return false;

        ClassifierDescriptor myDescriptor = me.getDeclarationDescriptor();
        ClassifierDescriptor otherDescriptor = ((TypeConstructor) other).getDeclarationDescriptor();

        // All error types have the same descriptor
        if (myDescriptor != null && !hasMeaningfulFqName(myDescriptor) ||
            otherDescriptor != null && !hasMeaningfulFqName(otherDescriptor)) {
            return me == other;
        }

        if (myDescriptor == otherDescriptor) return true;

        if (myDescriptor instanceof ClassDescriptor && otherDescriptor instanceof ClassDescriptor) {
            FqNameUnsafe otherFqName = DescriptorUtils.getFqName(otherDescriptor);
            FqNameUnsafe myFqName = DescriptorUtils.getFqName(myDescriptor);
            return myFqName.equals(otherFqName);
        }

        return false;
    }

    public static int hashCode(@NotNull TypeConstructor me) {
        ClassifierDescriptor descriptor = me.getDeclarationDescriptor();
        if (descriptor instanceof ClassDescriptor && hasMeaningfulFqName(descriptor)) {
            return DescriptorUtils.getFqName(descriptor).hashCode();
        }
        return System.identityHashCode(me);
    }

    private static boolean hasMeaningfulFqName(@NotNull ClassifierDescriptor descriptor) {
        return !ErrorUtils.isError(descriptor) &&
               !DescriptorUtils.isLocal(descriptor);
    }
}

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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.state.TypeMapperUtilsKt;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

public class SamType {
    @Nullable
    public static SamType createByValueParameter(@NotNull ValueParameterDescriptor valueParameter) {
        KotlinType originalTypeToUse =
                // This can be true in case when the value parameter is in the method of a generic type with out-projection.
                // We approximate Inv<Captured#1> to Nothing, while Inv itself can be a SAM interface safe to call here
                // (see testData genericSamProjectedOut.kt for details)
                KotlinBuiltIns.isNothing(valueParameter.getType())
                // In such a case we can't have a proper supertype since wildcards are not allowed there,
                // so we use Nothing arguments instead that leads to a raw type used for a SAM wrapper.
                // See org.jetbrains.kotlin.codegen.state.KotlinTypeMapper#writeGenericType to understand how
                // raw types and Nothing arguments relate.
                ? TypeUtilsKt.replaceArgumentsWithNothing(valueParameter.getOriginal().getType())
                : valueParameter.getType();

        return create(TypeMapperUtilsKt.removeExternalProjections(originalTypeToUse));
    }
    public static SamType create(@NotNull KotlinType originalType) {
        if (!SingleAbstractMethodUtils.isSamType(originalType)) return null;
        return new SamType(originalType);
    }

    private final KotlinType type;

    private SamType(@NotNull KotlinType type) {
        this.type = type;
    }

    @NotNull
    public KotlinType getType() {
        return type;
    }

    @NotNull
    public ClassDescriptor getClassDescriptor() {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        assert classifier instanceof ClassDescriptor : "Sam/Fun interface not a class descriptor: " + classifier;
        return (ClassDescriptor) classifier;
    }

    @NotNull
    public KotlinType getKotlinFunctionType() {
        ClassDescriptor descriptor = getClassDescriptor();
        //noinspection ConstantConditions
        return descriptor.getDefaultFunctionTypeForSamInterface();
    }

    @NotNull
    public SimpleFunctionDescriptor getOriginalAbstractMethod() {
        return (SimpleFunctionDescriptor) SingleAbstractMethodUtils.getAbstractMembers(getClassDescriptor()).get(0);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SamType && type.equals(((SamType) o).type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "SamType(" + type + ")";
    }
}

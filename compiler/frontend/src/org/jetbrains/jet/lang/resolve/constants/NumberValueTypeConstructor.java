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

package org.jetbrains.jet.lang.resolve.constants;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NumberValueTypeConstructor implements TypeConstructor {
    private final long value;
    private final Collection<JetType> supertypes = Lists.newArrayList();

    public NumberValueTypeConstructor(long value) {
        this.value = value;

        checkBoundsAndSuperType((long)Byte.MIN_VALUE, (long)Byte.MAX_VALUE, KotlinBuiltIns.getInstance().getByteType());
        checkBoundsAndSuperType((long)Short.MIN_VALUE, (long)Short.MAX_VALUE, KotlinBuiltIns.getInstance().getShortType());
        checkBoundsAndSuperType((long)Integer.MIN_VALUE, (long)Integer.MAX_VALUE, KotlinBuiltIns.getInstance().getIntType());
        supertypes.add(KotlinBuiltIns.getInstance().getLongType());
    }

    private void checkBoundsAndSuperType(long minValue, long maxValue, JetType kotlinType) {
        if (value > minValue && value < maxValue) {
            supertypes.add(kotlinType);
        }
    }

    public Long getValue() {
        return value;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public boolean isDenotable() {
        return false;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getDeclarationDescriptor() {
        return null;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NumberValueTypeConstructor that = (NumberValueTypeConstructor) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return "NumberValueTypeConstructor(" + value + ')';
    }
}

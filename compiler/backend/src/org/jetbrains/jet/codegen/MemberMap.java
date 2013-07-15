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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class MemberMap {
    private final Map<FunctionDescriptor, Method> methodForFunction = new HashMap<FunctionDescriptor, Method>();
    private final Map<PropertyDescriptor, Pair<Type, String>> fieldForProperty = new HashMap<PropertyDescriptor, Pair<Type, String>>();
    private final Map<PropertyDescriptor, String> syntheticMethodNameForProperty = new HashMap<PropertyDescriptor, String>();
    private final Map<CallableMemberDescriptor, Name> srcClassNameForCallable = new HashMap<CallableMemberDescriptor, Name>();

    @NotNull
    public static MemberMap union(@NotNull Collection<MemberMap> maps) {
        MemberMap result = new MemberMap();
        for (MemberMap map : maps) {
            for (Map.Entry<FunctionDescriptor, Method> entry : map.methodForFunction.entrySet()) {
                result.recordMethodOfDescriptor(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<PropertyDescriptor, Pair<Type, String>> entry : map.fieldForProperty.entrySet()) {
                result.recordFieldOfProperty(entry.getKey(), entry.getValue().first, entry.getValue().second);
            }

            for (Map.Entry<PropertyDescriptor, String> entry : map.syntheticMethodNameForProperty.entrySet()) {
                result.recordSyntheticMethodNameOfProperty(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<CallableMemberDescriptor, Name> entry : map.srcClassNameForCallable.entrySet()) {
                result.recordSrcClassNameForCallable(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public void recordMethodOfDescriptor(@NotNull FunctionDescriptor descriptor, @NotNull Method method) {
        Method old = methodForFunction.put(descriptor, method);
        assert old == null : "Duplicate method for callable member: " + descriptor + "; " + old;
    }

    public void recordFieldOfProperty(@NotNull PropertyDescriptor descriptor, @NotNull Type type, @NotNull String fieldName) {
        Pair<Type, String> old = fieldForProperty.put(descriptor, Pair.create(type, fieldName));
        assert old == null : "Duplicate field for property: " + descriptor + "; " + old;
    }

    public void recordSyntheticMethodNameOfProperty(@NotNull PropertyDescriptor descriptor, @NotNull String name) {
        String old = syntheticMethodNameForProperty.put(descriptor, name);
        assert old == null : "Duplicate synthetic method for property: " + descriptor + "; " + old;
    }

    public void recordSrcClassNameForCallable(@NotNull CallableMemberDescriptor descriptor, @NotNull Name name) {
        Name old = srcClassNameForCallable.put(descriptor, name);
        assert old == null : "Duplicate src class name for callable: " + descriptor + "; " + old;
    }

    @Nullable
    public Method getMethodOfDescriptor(@NotNull FunctionDescriptor descriptor) {
        return methodForFunction.get(descriptor);
    }

    @Nullable
    public Pair<Type, String> getFieldOfProperty(@NotNull PropertyDescriptor descriptor) {
        return fieldForProperty.get(descriptor);
    }

    @Nullable
    public String getSyntheticMethodNameOfProperty(@NotNull PropertyDescriptor descriptor) {
        return syntheticMethodNameForProperty.get(descriptor);
    }

    @Nullable
    public Name getSrcClassNameOfCallable(@NotNull CallableMemberDescriptor descriptor) {
        return srcClassNameForCallable.get(descriptor);
    }

    @Override
    public String toString() {
        return "Functions: " + methodForFunction.size() +
               ", fields: " + fieldForProperty.size() +
               ", synthetic methods: " + syntheticMethodNameForProperty.size() +
               ", src class names: " + srcClassNameForCallable.size();
    }
}

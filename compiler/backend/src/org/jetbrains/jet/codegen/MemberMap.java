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

import java.util.*;

public final class MemberMap {
    private final Map<FunctionDescriptor, Method> methodForFunction = new HashMap<FunctionDescriptor, Method>();
    private final Map<PropertyDescriptor, Pair<Type, String>> fieldForProperty = new HashMap<PropertyDescriptor, Pair<Type, String>>();
    private final Map<PropertyDescriptor, Method> syntheticMethodForProperty = new HashMap<PropertyDescriptor, Method>();
    private final Map<CallableMemberDescriptor, String> implClassNameForCallable = new HashMap<CallableMemberDescriptor, String>();
    private final Set<PropertyDescriptor> staticFieldInOuterClass = new HashSet<PropertyDescriptor>();

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

            for (Map.Entry<PropertyDescriptor, Method> entry : map.syntheticMethodForProperty.entrySet()) {
                result.recordSyntheticMethodOfProperty(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<CallableMemberDescriptor, String> entry : map.implClassNameForCallable.entrySet()) {
                result.recordImplClassNameForCallable(entry.getKey(), entry.getValue());
            }

            for (PropertyDescriptor property : map.staticFieldInOuterClass) {
                result.recordStaticFieldInOuterClass(property);
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

    public void recordSyntheticMethodOfProperty(@NotNull PropertyDescriptor descriptor, @NotNull Method method) {
        Method old = syntheticMethodForProperty.put(descriptor, method);
        assert old == null : "Duplicate synthetic method for property: " + descriptor + "; " + old;
    }

    public void recordImplClassNameForCallable(@NotNull CallableMemberDescriptor descriptor, @NotNull String name) {
        String old = implClassNameForCallable.put(descriptor, name);
        assert old == null : "Duplicate src class name for callable: " + descriptor + "; " + old;
    }

    public void recordStaticFieldInOuterClass(@NotNull PropertyDescriptor property) {
        boolean added = staticFieldInOuterClass.add(property);
        assert added : "Duplicate static field in outer class: " + property;
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
    public Method getSyntheticMethodOfProperty(@NotNull PropertyDescriptor descriptor) {
        return syntheticMethodForProperty.get(descriptor);
    }

    @Nullable
    public String getImplClassNameOfCallable(@NotNull CallableMemberDescriptor descriptor) {
        return implClassNameForCallable.get(descriptor);
    }

    public boolean isStaticFieldInOuterClass(@NotNull PropertyDescriptor property) {
        return staticFieldInOuterClass.contains(property);
    }

    @Override
    public String toString() {
        return "Functions: " + methodForFunction.size() +
               ", fields: " + fieldForProperty.size() +
               ", synthetic methods: " + syntheticMethodForProperty.size() +
               ", impl class names: " + implClassNameForCallable.size();
    }
}

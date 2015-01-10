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
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.kotlin.types.JetType;

public class SamType {
    public static SamType create(@NotNull JetType originalType) {
        if (!SingleAbstractMethodUtils.isSamType(originalType)) return null;
        return new SamType(originalType);
    }

    private final JetType type;

    private SamType(@NotNull JetType type) {
        this.type = type;
    }

    @NotNull
    public JetType getType() {
        return type;
    }

    @NotNull
    public JavaClassDescriptor getJavaClassDescriptor() {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        assert classifier instanceof JavaClassDescriptor : "Sam interface not a Java class: " + classifier;
        return (JavaClassDescriptor) classifier;
    }

    @NotNull
    public JetType getKotlinFunctionType() {
        //noinspection ConstantConditions
        return getJavaClassDescriptor().getFunctionTypeForSamInterface();
    }

    @NotNull
    public SimpleFunctionDescriptor getAbstractMethod() {
        return (SimpleFunctionDescriptor) SingleAbstractMethodUtils.getAbstractMembers(type).get(0);
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

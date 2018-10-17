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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

public final class EnclosedValueDescriptor {
    private final String fieldName;
    private final DeclarationDescriptor descriptor;
    private final StackValue.StackValueWithSimpleReceiver innerValue;
    private final StackValue instanceValue;
    private final Type type;
    private final KotlinType kotlinType;

    public EnclosedValueDescriptor(
            @NotNull String fieldName,
            @Nullable DeclarationDescriptor descriptor,
            @NotNull StackValue.StackValueWithSimpleReceiver innerValue,
            @NotNull Type type,
            @Nullable KotlinType kotlinType
    ) {
        this.fieldName = fieldName;
        this.descriptor = descriptor;
        this.innerValue = innerValue;
        this.instanceValue = innerValue;
        this.type = type;
        this.kotlinType = kotlinType;
    }

    public EnclosedValueDescriptor(
            @NotNull String name,
            @Nullable DeclarationDescriptor descriptor,
            @NotNull StackValue.StackValueWithSimpleReceiver innerValue,
            @NotNull StackValue.Field instanceValue,
            @NotNull Type type,
            @Nullable KotlinType kotlinType
    ) {
        this.fieldName = name;
        this.descriptor = descriptor;
        this.innerValue = innerValue;
        this.instanceValue = instanceValue;
        this.type = type;
        this.kotlinType = kotlinType;
    }

    @NotNull
    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    @NotNull
    public StackValue.StackValueWithSimpleReceiver getInnerValue() {
        return innerValue;
    }

    @NotNull
    public StackValue getInstanceValue() {
        return instanceValue;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public KotlinType getKotlinType() {
        return kotlinType;
    }

    @Override
    public String toString() {
        return fieldName + " " + type + " -> " + descriptor;
    }
}

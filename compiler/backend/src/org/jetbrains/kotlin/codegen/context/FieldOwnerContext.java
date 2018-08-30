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
import org.jetbrains.kotlin.codegen.AccessorForPropertyDescriptor;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;

import java.util.HashMap;
import java.util.Map;

public abstract class FieldOwnerContext<T extends DeclarationDescriptor> extends CodegenContext<T> {
    //default property name -> map<property descriptor -> bytecode name>
    private final Map<String, Map<PropertyDescriptor, String>> fieldNames = new HashMap<>();

    public FieldOwnerContext(
            @NotNull T contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable MutableClosure closure,
            @Nullable ClassDescriptor thisDescriptor,
            @Nullable LocalLookup expressionCodegen
    ) {
        super(contextDescriptor, contextKind, parentContext, closure, thisDescriptor, expressionCodegen);
    }

    @NotNull
    public String getFieldName(@NotNull PropertyDescriptor possiblySubstitutedDescriptor, boolean isDelegated) {
        if (possiblySubstitutedDescriptor instanceof AccessorForPropertyDescriptor) {
            possiblySubstitutedDescriptor = ((AccessorForPropertyDescriptor) possiblySubstitutedDescriptor).getCalleeDescriptor();
        }

        PropertyDescriptor descriptor = possiblySubstitutedDescriptor.getOriginal();
        assert descriptor.getKind().isReal() : "Only declared properties can have backing fields: " + descriptor;

        String defaultPropertyName = KotlinTypeMapper.mapDefaultFieldName(descriptor, isDelegated);

        Map<PropertyDescriptor, String> descriptor2Name = fieldNames.computeIfAbsent(defaultPropertyName, unused -> new HashMap<>());

        String actualName = descriptor2Name.get(descriptor);
        if (actualName != null) return actualName;

        String newName = descriptor2Name.isEmpty() || JvmAnnotationUtilKt.hasJvmFieldAnnotation(descriptor)
                         ? defaultPropertyName
                         : defaultPropertyName + "$" + descriptor2Name.size();
        descriptor2Name.put(descriptor, newName);
        return newName;
    }
}

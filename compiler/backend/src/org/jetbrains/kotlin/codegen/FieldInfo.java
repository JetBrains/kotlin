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
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.org.objectweb.asm.Type;

public class FieldInfo {

    @NotNull
    public static FieldInfo createForCompanionSingleton(@NotNull ClassDescriptor companionObject, @NotNull JetTypeMapper typeMapper) {
        ClassDescriptor ownerDescriptor = DescriptorUtils.getParentOfType(companionObject, ClassDescriptor.class);
        assert ownerDescriptor != null : "Owner not found for class: " + companionObject;
        Type ownerType = typeMapper.mapType(ownerDescriptor);
        return new FieldInfo(ownerType, typeMapper.mapType(companionObject), companionObject.getName().asString(), true);
    }

    @NotNull
    public static FieldInfo createForSingleton(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeMapper typeMapper) {
        return createForSingleton(classDescriptor, typeMapper, false);
    }

    @NotNull
    public static FieldInfo createForSingleton(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeMapper typeMapper, boolean oldSingleton) {
        if (!classDescriptor.getKind().isSingleton()) {
            throw new UnsupportedOperationException("Can't create singleton field for class: " + classDescriptor);
        }
        Type type = typeMapper.mapType(classDescriptor);
        return new FieldInfo(type, type, oldSingleton ? JvmAbi.DEPRECATED_INSTANCE_FIELD : JvmAbi.INSTANCE_FIELD, true);
    }

    @NotNull
    public static FieldInfo createForHiddenField(@NotNull Type owner, @NotNull Type fieldType, @NotNull String fieldName) {
        return new FieldInfo(owner, fieldType, fieldName, false);
    }

    private final Type fieldType;
    private final Type ownerType;
    private final String fieldName;
    private final boolean isStatic;

    private FieldInfo(@NotNull Type ownerType, @NotNull Type fieldType, @NotNull String fieldName, boolean isStatic) {
        this.ownerType = ownerType;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.isStatic = isStatic;
    }

    @NotNull
    public Type getFieldType() {
        return fieldType;
    }

    @NotNull
    public Type getOwnerType() {
        return ownerType;
    }

    @NotNull
    public String getOwnerInternalName() {
        return ownerType.getInternalName();
    }

    @NotNull
    public String getFieldName() {
        return fieldName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        return String.format("%s %s.%s : %s", isStatic ? "GETSTATIC" : "GETFIELD", ownerType.getInternalName(), fieldName, fieldType);
    }
}

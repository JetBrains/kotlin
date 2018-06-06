/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.CompanionObjectMapping;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isNonCompanionObject;

public class FieldInfo {
    @NotNull
    public static FieldInfo createForSingleton(@NotNull ClassDescriptor classDescriptor, @NotNull KotlinTypeMapper typeMapper) {
        if (!classDescriptor.getKind().isSingleton() || DescriptorUtils.isEnumEntry(classDescriptor)) {
            throw new UnsupportedOperationException("Can't create singleton field for class: " + classDescriptor);
        }

        if (isNonCompanionObject(classDescriptor) || CompanionObjectMapping.INSTANCE.isMappedIntrinsicCompanionObject(classDescriptor)) {
            return createSingletonViaInstance(classDescriptor, typeMapper, JvmAbi.INSTANCE_FIELD);
        }

        ClassDescriptor ownerDescriptor = DescriptorUtils.getParentOfType(classDescriptor, ClassDescriptor.class);
        assert ownerDescriptor != null : "Owner not found for class: " + classDescriptor;
        Type ownerType = typeMapper.mapClass(ownerDescriptor);
        return new FieldInfo(ownerType, typeMapper.mapType(classDescriptor), classDescriptor.getName().asString(), true);
    }

    @NotNull
    public static FieldInfo createSingletonViaInstance(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull String name
    ) {
        Type type = typeMapper.mapType(classDescriptor);
        return new FieldInfo(type, type, name, true);
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

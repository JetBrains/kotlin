/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.CompanionObjectMapping;
import org.jetbrains.kotlin.builtins.CompanionObjectMappingUtilsKt;
import org.jetbrains.kotlin.descriptors.*;

import static org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isClassOrEnumClass;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;

public final class DescriptorsJvmAbiUtil {
    public static boolean isPropertyWithBackingFieldInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        if (propertyDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return false;

        if (isClassCompanionObjectWithBackingFieldsInOuter(propertyDescriptor.getContainingDeclaration())) return true;

        return isCompanionObject(propertyDescriptor.getContainingDeclaration()) && hasJvmFieldAnnotation(propertyDescriptor);
    }

    public static boolean isClassCompanionObjectWithBackingFieldsInOuter(@NotNull DeclarationDescriptor companionObject) {
        return isCompanionObject(companionObject) &&
               isClassOrEnumClass(companionObject.getContainingDeclaration()) &&
               !isMappedIntrinsicCompanionObject((ClassDescriptor) companionObject);
    }

    public static boolean isMappedIntrinsicCompanionObject(@NotNull ClassDescriptor companionObject) {
        return CompanionObjectMappingUtilsKt.isMappedIntrinsicCompanionObject(CompanionObjectMapping.INSTANCE, companionObject);
    }

    public static boolean hasJvmFieldAnnotation(@NotNull CallableMemberDescriptor memberDescriptor) {
        // TODO: deduplicate this with org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
        if (memberDescriptor instanceof PropertyDescriptor) {
            FieldDescriptor field = ((PropertyDescriptor) memberDescriptor).getBackingField();
            if (field != null && field.getAnnotations().hasAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)) return true;
        }
        return memberDescriptor.getAnnotations().hasAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME);
    }
}

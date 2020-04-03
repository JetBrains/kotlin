/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.CompanionObjectMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.util.capitalizeDecapitalize.CapitalizeDecapitalizeKt;

import java.util.List;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isClassOrEnumClass;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;

public final class JvmAbi {
    public static final String DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls";
    public static final String ERASED_INLINE_CONSTRUCTOR_NAME = "constructor";
    public static final FqName JVM_FIELD_ANNOTATION_FQ_NAME = new FqName("kotlin.jvm.JvmField");

    /**
     * Warning: use DEFAULT_IMPLS_CLASS_NAME and TypeMappingConfiguration.innerClassNameFactory when possible.
     * This is false for KAPT3 mode.
     */
    public static final String DEFAULT_IMPLS_SUFFIX = "$" + DEFAULT_IMPLS_CLASS_NAME;

    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String SET_PREFIX = "set";

    public static final String DELEGATED_PROPERTY_NAME_SUFFIX = "$delegate";
    public static final String DELEGATED_PROPERTIES_ARRAY_NAME = "$$delegatedProperties";
    public static final String DELEGATE_SUPER_FIELD_PREFIX = "$$delegate_";
    private static final String ANNOTATIONS_SUFFIX = "$annotations";
    public static final String ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX = ANNOTATIONS_SUFFIX;
    private static final String ANNOTATED_TYPEALIAS_METHOD_NAME_SUFFIX = ANNOTATIONS_SUFFIX;

    public static final String INSTANCE_FIELD = "INSTANCE";
    public static final String HIDDEN_INSTANCE_FIELD = "$$" + INSTANCE_FIELD;

    public static final ClassId REFLECTION_FACTORY_IMPL = ClassId.topLevel(new FqName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl"));

    /*
        Line number is used when inlining function arguments to @InlineOnly functions.
        All line information is stripped from the inlined implementation of such functions,
        so if a lambda is on the same line with the call, the debugger can't stop on both 'line' and 'lambda' breakpoints.

        Example:
            > require(foo) { "foo is false" }
     */
    public static final int LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER = 65100;

    public static final List<Integer> SYNTHETIC_MARKER_LINE_NUMBERS = CollectionsKt.listOf(
            LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER
    );

    public static final String LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT = "$i$a$";
    public static final String LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION = "$i$f$";

    public static final String IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS = "-impl";

    /**
     * @param baseName JVM name of the property getter since Kotlin 1.4, or Kotlin name of the property otherwise.
     */
    @NotNull
    public static String getSyntheticMethodNameForAnnotatedProperty(@NotNull String baseName) {
        return baseName + ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX;
    }

    @NotNull
    public static String getSyntheticMethodNameForAnnotatedTypeAlias(@NotNull Name typeAliasName) {
        return typeAliasName.asString() + ANNOTATED_TYPEALIAS_METHOD_NAME_SUFFIX;
    }

    public static boolean isGetterName(@NotNull String name) {
        return name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX);
    }

    public static boolean isSetterName(@NotNull String name) {
        return name.startsWith(SET_PREFIX);
    }

    @NotNull
    public static String getterName(@NotNull String propertyName) {
        return startsWithIsPrefix(propertyName)
               ? propertyName
               : GET_PREFIX + CapitalizeDecapitalizeKt.capitalizeAsciiOnly(propertyName);
    }

    @NotNull
    public static String setterName(@NotNull String propertyName) {
        return SET_PREFIX +
               (startsWithIsPrefix(propertyName)
                ? propertyName.substring(IS_PREFIX.length())
                : CapitalizeDecapitalizeKt.capitalizeAsciiOnly(propertyName));
    }

    public static boolean startsWithIsPrefix(String name) {
        if (!name.startsWith(IS_PREFIX)) return false;
        if (name.length() == IS_PREFIX.length()) return false;
        char c = name.charAt(IS_PREFIX.length());
        return !('a' <= c && c <= 'z');
    }

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
        return CompanionObjectMapping.INSTANCE.isMappedIntrinsicCompanionObject(companionObject);
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

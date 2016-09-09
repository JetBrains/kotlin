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

package org.jetbrains.kotlin.load.java;

import kotlin.text.Regex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.CompanionObjectMapping;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.util.capitalizeDecapitalize.CapitalizeDecapitalizeKt;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isClassOrEnumClass;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;

public final class JvmAbi {
    public static final String DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls";
    public static final String DEFAULT_IMPLS_SUFFIX = "$" + DEFAULT_IMPLS_CLASS_NAME;
    public static final String DEFAULT_IMPLS_DELEGATE_SUFFIX = "$defaultImpl";

    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String SET_PREFIX = "set";

    public static final String DELEGATED_PROPERTY_NAME_SUFFIX = "$delegate";
    public static final String DELEGATED_PROPERTIES_ARRAY_NAME = "$$delegatedProperties";
    public static final String DELEGATE_SUPER_FIELD_PREFIX = "$$delegate_";
    public static final String ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX = "$annotations";

    public static final String INSTANCE_FIELD = "INSTANCE";

    public static final String DEFAULT_MODULE_NAME = "main";
    public static final ClassId REFLECTION_FACTORY_IMPL = ClassId.topLevel(new FqName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl"));

    public static final String LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT = "$i$a$";
    public static final String LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION = "$i$f$";

    private static final Regex SANITIZE_AS_JAVA_INVALID_CHARACTERS = new Regex("[^\\p{L}\\p{Digit}]");

    @NotNull
    public static String getSyntheticMethodNameForAnnotatedProperty(@NotNull Name propertyName) {
        return propertyName.asString() + ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX;
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
        return startsWithIsPrefix(propertyName)
               ? SET_PREFIX + propertyName.substring(IS_PREFIX.length())
               : SET_PREFIX + CapitalizeDecapitalizeKt.capitalizeAsciiOnly(propertyName);
    }

    public static boolean startsWithIsPrefix(String name) {
        if (!name.startsWith(IS_PREFIX)) return false;
        if (name.length() == IS_PREFIX.length()) return false;
        char c = name.charAt(IS_PREFIX.length());
        return !('a' <= c && c <= 'z');
    }

    @NotNull
    public static String sanitizeAsJavaIdentifier(@NotNull String str) {
        return SANITIZE_AS_JAVA_INVALID_CHARACTERS.replace(str, "_");
    }

    public static boolean isPropertyWithBackingFieldInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
               isCompanionObjectWithBackingFieldsInOuter(propertyDescriptor.getContainingDeclaration());
    }

    public static boolean isCompanionObjectWithBackingFieldsInOuter(@NotNull DeclarationDescriptor companionObject) {
        return isCompanionObject(companionObject) &&
               isClassOrEnumClass(companionObject.getContainingDeclaration()) &&
               !CompanionObjectMapping.INSTANCE.isMappedIntrinsicCompanionObject((ClassDescriptor) companionObject);
    }
}

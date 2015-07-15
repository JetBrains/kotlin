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

import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

public final class JvmAbi {
    /**
     * This constant is used to identify binary format (class file) versions
     * If you change class file metadata format and/or naming conventions, please increase this number
     */
    public static final int VERSION = 23;

    public static final String TRAIT_IMPL_CLASS_NAME = "$TImpl";
    public static final String TRAIT_IMPL_SUFFIX = "$" + TRAIT_IMPL_CLASS_NAME;

    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";

    public static final String DELEGATED_PROPERTY_NAME_SUFFIX = "$delegate";
    public static final String PROPERTY_METADATA_ARRAY_NAME = "$propertyMetadata";
    public static final String ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX = "$annotations";

    public static final String INSTANCE_FIELD = "INSTANCE$";

    public static final String KOTLIN_CLASS_FIELD_NAME = "$kotlinClass";
    public static final String KOTLIN_PACKAGE_FIELD_NAME = "$kotlinPackage";
    public static final ClassId REFLECTION_FACTORY_IMPL = ClassId.topLevel(new FqName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl"));

    @NotNull
    public static String getSyntheticMethodNameForAnnotatedProperty(@NotNull Name propertyName) {
        return propertyName.asString() + ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX;
    }

    @NotNull
    public static String getDefaultFieldNameForProperty(@NotNull Name propertyName, boolean isDelegated) {
        return isDelegated ? propertyName.asString() + DELEGATED_PROPERTY_NAME_SUFFIX : propertyName.asString();
    }

    @NotNull
    public static String getterName(@NotNull String propertyName) {
        return GETTER_PREFIX + capitalizeWithJavaBeanConvention(propertyName);
    }

    @NotNull
    public static String setterName(@NotNull String propertyName) {
        return SETTER_PREFIX + capitalizeWithJavaBeanConvention(propertyName);
    }

    /**
     * @see com.intellij.openapi.util.text.StringUtil#capitalizeWithJavaBeanConvention(String)
     */
    @NotNull
    private static String capitalizeWithJavaBeanConvention(@NotNull String s) {
        return s.length() > 1 && Character.isUpperCase(s.charAt(1)) ? s : KotlinPackage.capitalize(s);
    }
}

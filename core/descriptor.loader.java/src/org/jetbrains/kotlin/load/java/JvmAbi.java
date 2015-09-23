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
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion;

public final class JvmAbi {
    /**
     * This constant is used to identify binary format (class file) versions
     * If you change class file metadata format and/or naming conventions, please change this version.
     * - Major version should be increased only when the new binary format is neither forward- nor backward compatible.
     *   This shouldn't really ever happen at all.
     * - Minor version should be increased when the new format is backward compatible,
     *   i.e. the new compiler can process old class files, but the old compiler will not be able to process new class files.
     * - Patch version can be increased freely and is only supposed to be used for debugging. Increase the patch version when you
     *   make a change to the metadata format or the bytecode which is both forward- and backward compatible.
     */
    public static final BinaryVersion VERSION = BinaryVersion.create(0, 26, 0);

    public static final String TRAIT_IMPL_CLASS_NAME = "$TImpl";
    public static final String TRAIT_IMPL_SUFFIX = "$" + TRAIT_IMPL_CLASS_NAME;

    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String SET_PREFIX = "set";

    public static final String DELEGATED_PROPERTY_NAME_SUFFIX = "$delegate";
    public static final String PROPERTY_METADATA_ARRAY_NAME = "$propertyMetadata";
    public static final String ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX = "$annotations";

    public static final String INSTANCE_FIELD = "INSTANCE$";

    public static final String KOTLIN_CLASS_FIELD_NAME = "$kotlinClass";
    public static final String KOTLIN_PACKAGE_FIELD_NAME = "$kotlinPackage";
    public static final String MODULE_NAME_FIELD = "$moduleName";
    public static final String DEFAULT_MODULE_NAME = "main";
    public static final ClassId REFLECTION_FACTORY_IMPL = ClassId.topLevel(new FqName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl"));

    @NotNull
    public static String getSyntheticMethodNameForAnnotatedProperty(@NotNull Name propertyName) {
        return propertyName.asString() + ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX;
    }

    @NotNull
    public static String getDefaultFieldNameForProperty(@NotNull Name propertyName, boolean isDelegated) {
        return isDelegated ? propertyName.asString() + DELEGATED_PROPERTY_NAME_SUFFIX : propertyName.asString();
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
               : GET_PREFIX + KotlinPackage.capitalize(propertyName);

    }

    @NotNull
    public static String setterName(@NotNull String propertyName) {
        return startsWithIsPrefix(propertyName)
               ? SET_PREFIX + propertyName.substring(IS_PREFIX.length())
               : SET_PREFIX + KotlinPackage.capitalize(propertyName);
    }

    private static boolean startsWithIsPrefix(String name) {
        if (!name.startsWith(IS_PREFIX)) return false;
        if (name.length() == IS_PREFIX.length()) return false;
        char c = name.charAt(IS_PREFIX.length());
        return !Character.isLowerCase(c);
    }
}


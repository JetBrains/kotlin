/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public final class JvmAnnotationNames {
    public static final FqName KOTLIN_CLASS = new FqName("kotlin.jvm.internal.KotlinClass");
    public static final FqName KOTLIN_PACKAGE = new FqName("kotlin.jvm.internal.KotlinPackage");
    public static final FqName KOTLIN_SYNTHETIC_CLASS = new FqName("kotlin.jvm.internal.KotlinSyntheticClass");

    public static final FqName KOTLIN_SIGNATURE = new FqName("kotlin.jvm.KotlinSignature");
    public static final FqName OLD_KOTLIN_SIGNATURE = new FqName("jet.runtime.typeinfo.KotlinSignature");

    public static final String ABI_VERSION_FIELD_NAME = "abiVersion";
    public static final String DATA_FIELD_NAME = "data";
    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");

    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = new FqName("org.jetbrains.annotations.NotNull");
    public static final FqName JETBRAINS_NULLABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Nullable");
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");

    @Deprecated
    public static final FqName OLD_JET_CLASS_ANNOTATION = new FqName("jet.runtime.typeinfo.JetClass");
    @Deprecated
    public static final FqName OLD_JET_PACKAGE_CLASS_ANNOTATION = new FqName("jet.runtime.typeinfo.JetPackageClass");
    @Deprecated
    public static final FqName OLD_JET_VALUE_PARAMETER_ANNOTATION = new FqName("jet.runtime.typeinfo.JetValueParameter");
    @Deprecated
    public static final FqName OLD_KOTLIN_CLASS = new FqName("jet.KotlinClass");
    @Deprecated
    public static final FqName OLD_KOTLIN_PACKAGE = new FqName("jet.KotlinPackage");
    @Deprecated
    public static final FqName OLD_KOTLIN_PACKAGE_FRAGMENT = new FqName("jet.KotlinPackageFragment");
    @Deprecated
    public static final FqName OLD_KOTLIN_TRAIT_IMPL = new FqName("jet.KotlinTraitImpl");

    @SuppressWarnings("deprecation")
    public static boolean isSpecialAnnotation(@NotNull FqName fqName) {
        return fqName.asString().startsWith("jet.runtime.typeinfo.")
               || fqName.equals(KOTLIN_SIGNATURE)
               || fqName.equals(JETBRAINS_NOT_NULL_ANNOTATION)
               || fqName.equals(OLD_KOTLIN_CLASS)
               || fqName.equals(OLD_KOTLIN_PACKAGE)
               || fqName.equals(KOTLIN_CLASS)
               || fqName.equals(KOTLIN_PACKAGE);
    }

    private JvmAnnotationNames() {
    }
}

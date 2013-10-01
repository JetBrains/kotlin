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

import jet.KotlinClass;
import jet.KotlinPackage;
import jet.KotlinPackageFragment;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

public final class JvmAnnotationNames {
    public static final JvmClassName KOTLIN_CLASS = JvmClassName.byClass(KotlinClass.class);

    public static final JvmClassName KOTLIN_PACKAGE = JvmClassName.byClass(KotlinPackage.class);

    public static final JvmClassName KOTLIN_PACKAGE_FRAGMENT = JvmClassName.byClass(KotlinPackageFragment.class);

    public static final String ABI_VERSION_FIELD_NAME = "abiVersion";

    public static final String DATA_FIELD_NAME = "data";
    @Deprecated
    public static final JvmClassName OLD_JET_CLASS_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetClass");
    @Deprecated
    public static final JvmClassName OLD_JET_PACKAGE_CLASS_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetPackageClass");

    public static final JvmClassName ASSERT_INVISIBLE_IN_RESOLVER = JvmClassName.byClass(AssertInvisibleInResolver.class);

    public static final JvmClassName KOTLIN_SIGNATURE = JvmClassName.byClass(KotlinSignature.class);

    public static final Name KOTLIN_SIGNATURE_VALUE_FIELD_NAME = Name.identifier("value");

    public static final JvmClassName JETBRAINS_NOT_NULL_ANNOTATION = JvmClassName.byClass(NotNull.class);

    public static final JvmClassName JETBRAINS_MUTABLE_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses("org.jetbrains.annotations.Mutable");

    public static final JvmClassName JETBRAINS_READONLY_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses("org.jetbrains.annotations.ReadOnly");

    private JvmAnnotationNames() {
    }
}

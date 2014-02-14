/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public final class AnnotationLoadingUtil {
    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");
    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = fqNameByClass(NotNull.class);
    public static final FqName JETBRAINS_NULLABLE_ANNOTATION = fqNameByClass(Nullable.class);
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");
    public static final FqName JL_CLASS_FQ_NAME = new FqName("java.lang.Class");

    @SuppressWarnings("deprecation")
    public static boolean isSpecialAnnotation(@NotNull FqName fqName) {
        return fqName.asString().startsWith("jet.runtime.typeinfo.")
            || fqName.equals(JETBRAINS_NOT_NULL_ANNOTATION)
            || fqName.equals(JvmAnnotationNames.OLD_KOTLIN_CLASS)
            || fqName.equals(JvmAnnotationNames.OLD_KOTLIN_PACKAGE)
            || fqName.equals(JvmAnnotationNames.KOTLIN_CLASS)
            || fqName.equals(JvmAnnotationNames.KOTLIN_PACKAGE);
    }

    private AnnotationLoadingUtil() {}
}

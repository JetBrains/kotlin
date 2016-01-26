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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class JvmAnnotationNames {
    public static final FqName METADATA = new FqName("kotlin.Metadata");

    public static final String METADATA_VERSION_FIELD_NAME = "mv";
    public static final String BYTECODE_VERSION_FIELD_NAME = "bv";
    public static final String KIND_FIELD_NAME = "k";
    public static final String METADATA_DATA_FIELD_NAME = "d1";
    public static final String METADATA_STRINGS_FIELD_NAME = "d2";
    public static final String METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME = "xs";
    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");
    public static final Name DEPRECATED_ANNOTATION_MESSAGE = Name.identifier("message");
    public static final Name TARGET_ANNOTATION_MEMBER_NAME = Name.identifier("allowedTargets");

    public static final FqName TARGET_ANNOTATION = new FqName("java.lang.annotation.Target");
    public static final FqName RETENTION_ANNOTATION = new FqName("java.lang.annotation.Retention");
    public static final FqName DOCUMENTED_ANNOTATION = new FqName("java.lang.annotation.Documented");

    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = new FqName("org.jetbrains.annotations.NotNull");
    public static final FqName JETBRAINS_NULLABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Nullable");
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");

    public static final FqName PURELY_IMPLEMENTS_ANNOTATION = new FqName("kotlin.jvm.PurelyImplements");

    // Just for internal use: there is no such real classes in bytecode
    public static final FqName ENHANCED_NULLABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedNullability");
    public static final FqName ENHANCED_MUTABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedMutability");

    private static final Set<JvmClassName> NULLABILITY_ANNOTATIONS = new HashSet<JvmClassName>();
    private static final Set<JvmClassName> SPECIAL_META_ANNOTATIONS = new HashSet<JvmClassName>();

    static {
        for (FqName fqName : Arrays.asList(JETBRAINS_NOT_NULL_ANNOTATION, JETBRAINS_NULLABLE_ANNOTATION)) {
            NULLABILITY_ANNOTATIONS.add(JvmClassName.byFqNameWithoutInnerClasses(fqName));
        }

        for (FqName fqName : Arrays.asList(TARGET_ANNOTATION, RETENTION_ANNOTATION, DOCUMENTED_ANNOTATION)) {
            SPECIAL_META_ANNOTATIONS.add(JvmClassName.byFqNameWithoutInnerClasses(fqName));
        }
    }

    public static boolean isSpecialAnnotation(@NotNull ClassId classId, boolean javaSpecificAnnotationsAreSpecial) {
        if (classId.asSingleFqName().equals(METADATA)) return true;

        if (javaSpecificAnnotationsAreSpecial) {
            JvmClassName className = JvmClassName.byClassId(classId);
            if (NULLABILITY_ANNOTATIONS.contains(className) || SPECIAL_META_ANNOTATIONS.contains(className)) return true;
        }

        return false;
    }

    private JvmAnnotationNames() {
    }
}

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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName

// old JSpecify annotations

val JSPECIFY_OLD_NULLABLE_ANNOTATION_FQ_NAME = FqName("org.jspecify.nullness.Nullable")

val JSPECIFY_OLD_NULL_MARKED_ANNOTATION_FQ_NAME = FqName("org.jspecify.nullness.NullMarked")

val JSPECIFY_OLD_NULLNESS_UNSPECIFIED_ANNOTATION_FQ_NAME = FqName("org.jspecify.nullness.NullnessUnspecified")

// JSpecify annotations

val JSPECIFY_NON_NULL_ANNOTATION_FQ_NAME = FqName("org.jspecify.annotations.NonNull")

val JSPECIFY_NULLABLE_ANNOTATION_FQ_NAME = FqName("org.jspecify.annotations.Nullable")

val JSPECIFY_NULL_MARKED_ANNOTATION_FQ_NAME = FqName("org.jspecify.annotations.NullMarked")

val JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION_FQ_NAME = FqName("org.jspecify.annotations.NullnessUnspecified")

val JSPECIFY_NULL_UNMARKED_ANNOTATION_FQ_NAME = FqName("org.jspecify.annotations.NullUnmarked")

// JSR-305 annotations

val JAVAX_TYPE_QUALIFIER_ANNOTATION_FQ_NAME = FqName("javax.annotation.meta.TypeQualifier")
val JAVAX_TYPE_QUALIFIER_NICKNAME_ANNOTATION_FQ_NAME = FqName("javax.annotation.meta.TypeQualifierNickname")
val JAVAX_TYPE_QUALIFIER_DEFAULT_ANNOTATION_FQ_NAME = FqName("javax.annotation.meta.TypeQualifierDefault")

val JAVAX_NONNULL_ANNOTATION_FQ_NAME = FqName("javax.annotation.Nonnull")

val JAVAX_NULLABLE_ANNOTATION_FQ_NAME = FqName("javax.annotation.Nullable")
val JAVAX_CHECK_FOR_NULL_ANNOTATION_FQ_NAME = FqName("javax.annotation.CheckForNull")

val JAVAX_PARAMETERS_ARE_NONNULL_BY_DEFAULT_ANNOTATION_FQ_NAME = FqName("javax.annotation.ParametersAreNonnullByDefault")

val JAVAX_PARAMETERS_ARE_NULLABLE_BY_DEFAULT_ANNOTATION_FQ_NAME = FqName("javax.annotation.ParametersAreNullableByDefault")

val BUILT_IN_TYPE_QUALIFIER_ANNOTATIONS = setOf(
    JAVAX_NONNULL_ANNOTATION_FQ_NAME,
    JAVAX_CHECK_FOR_NULL_ANNOTATION_FQ_NAME
)

// nullability/nullness annotations

val NOT_NULL_ANNOTATIONS: Set<FqName> = setOf(
    // JetBrains
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
    // JSpecify
    JSPECIFY_NON_NULL_ANNOTATION_FQ_NAME,
    // JSR-305
    // JAVAX_NONNULL_ANNOTATION_FQ_NAME (is processed separately due to specified behavior depending on its value parameter)
    // Android
    FqName("android.annotation.NonNull"),
    FqName("androidx.annotation.NonNull"),
    FqName("androidx.annotation.RecentlyNonNull"),
    FqName("android.support.annotation.NonNull"),
    FqName("com.android.annotations.NonNull"),
    // Checker Framework
    FqName("org.checkerframework.checker.nullness.compatqual.NonNullDecl"),
    FqName("org.checkerframework.checker.nullness.qual.NonNull"),
    // FindBugs & SpotBugs
    FqName("edu.umd.cs.findbugs.annotations.NonNull"),
    // RxJava
    FqName("io.reactivex.annotations.NonNull"),
    FqName("io.reactivex.rxjava3.annotations.NonNull"),
    // Eclipse JDT
    FqName("org.eclipse.jdt.annotation.NonNull"),
    // Lombok
    FqName("lombok.NonNull"),
)

val NULLABLE_ANNOTATIONS: Set<FqName> = setOf(
    // JetBrains
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
    // JSpecify
    JSPECIFY_OLD_NULLABLE_ANNOTATION_FQ_NAME,
    JSPECIFY_NULLABLE_ANNOTATION_FQ_NAME,
    // JSR-305
    JAVAX_NULLABLE_ANNOTATION_FQ_NAME,
    JAVAX_CHECK_FOR_NULL_ANNOTATION_FQ_NAME,
    // Android
    FqName("android.annotation.Nullable"),
    FqName("androidx.annotation.Nullable"),
    FqName("androidx.annotation.RecentlyNullable"),
    FqName("android.support.annotation.Nullable"),
    FqName("com.android.annotations.Nullable"),
    // Checker Framework
    FqName("org.checkerframework.checker.nullness.compatqual.NullableDecl"),
    FqName("org.checkerframework.checker.nullness.qual.Nullable"),
    // FindBugs & SpotBugs
    FqName("edu.umd.cs.findbugs.annotations.Nullable"),
    FqName("edu.umd.cs.findbugs.annotations.PossiblyNull"),
    FqName("edu.umd.cs.findbugs.annotations.CheckForNull"),
    // RxJava
    FqName("io.reactivex.annotations.Nullable"),
    FqName("io.reactivex.rxjava3.annotations.Nullable"),
    // Eclipse JDT
    FqName("org.eclipse.jdt.annotation.Nullable"),
)

val FORCE_FLEXIBILITY_ANNOTATIONS: Set<FqName> = setOf(
    // JSpecify
    JSPECIFY_OLD_NULLNESS_UNSPECIFIED_ANNOTATION_FQ_NAME,
    JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION_FQ_NAME,
)

val NULLABILITY_ANNOTATIONS: Set<FqName> = mutableSetOf<FqName>() +
        NOT_NULL_ANNOTATIONS +
        NULLABLE_ANNOTATIONS +
        JAVAX_NONNULL_ANNOTATION_FQ_NAME +
        JSPECIFY_OLD_NULL_MARKED_ANNOTATION_FQ_NAME +
        JSPECIFY_NULL_MARKED_ANNOTATION_FQ_NAME +
        JSPECIFY_NULL_UNMARKED_ANNOTATION_FQ_NAME

// mutability annotations

val READ_ONLY_ANNOTATIONS: Set<FqName> = setOf(
    JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION,
    JvmAnnotationNames.READONLY_ANNOTATION
)

val MUTABLE_ANNOTATIONS: Set<FqName> = setOf(
    JvmAnnotationNames.JETBRAINS_MUTABLE_ANNOTATION,
    JvmAnnotationNames.MUTABLE_ANNOTATION
)

// "java.lang.annotation -> kotlin.annotation" mapping

val javaToKotlinNameMap: Map<FqName, FqName> = mapOf(
    JvmAnnotationNames.TARGET_ANNOTATION to StandardNames.FqNames.target,
    JvmAnnotationNames.RETENTION_ANNOTATION to StandardNames.FqNames.retention,
    JvmAnnotationNames.DEPRECATED_ANNOTATION to StandardNames.FqNames.deprecated,
    JvmAnnotationNames.DOCUMENTED_ANNOTATION to StandardNames.FqNames.mustBeDocumented
)

// other

val UNDER_MIGRATION_ANNOTATION_FQ_NAME = FqName("kotlin.annotations.jvm.UnderMigration")

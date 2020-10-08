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

import org.jetbrains.kotlin.name.FqName

val JSPECIFY_NULLABLE = FqName("org.jspecify.annotations.Nullable")
val JSPECIFY_NOT_NULL = FqName("org.jspecify.annotations.NotNull")
val JSPECIFY_NULLNESS_UNKNOWN = FqName("org.jspecify.annotations.NullnessUnspecified")

val JSPECIFY_DEFAULT_NULLABLE = FqName("org.jspecify.annotations.DefaultNullable")
val JSPECIFY_DEFAULT_NOT_NULL = FqName("org.jspecify.annotations.DefaultNotNull")
val JSPECIFY_DEFAULT_NULLNESS_UNKNOWN = FqName("org.jspecify.annotations.DefaultNullnessUnspecified")

val NULLABLE_ANNOTATIONS = listOf(
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
    FqName("androidx.annotation.Nullable"),
    FqName("android.support.annotation.Nullable"),
    FqName("android.annotation.Nullable"),
    FqName("com.android.annotations.Nullable"),
    FqName("org.eclipse.jdt.annotation.Nullable"),
    FqName("org.checkerframework.checker.nullness.qual.Nullable"),
    FqName("javax.annotation.Nullable"),
    FqName("javax.annotation.CheckForNull"),
    FqName("edu.umd.cs.findbugs.annotations.CheckForNull"),
    FqName("edu.umd.cs.findbugs.annotations.Nullable"),
    FqName("edu.umd.cs.findbugs.annotations.PossiblyNull"),
    FqName("io.reactivex.annotations.Nullable")
)

val JAVAX_NONNULL_ANNOTATION = FqName("javax.annotation.Nonnull")
val JAVAX_CHECKFORNULL_ANNOTATION = FqName("javax.annotation.CheckForNull")

val NOT_NULL_ANNOTATIONS = listOf(
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
    FqName("edu.umd.cs.findbugs.annotations.NonNull"),
    FqName("androidx.annotation.NonNull"),
    FqName("android.support.annotation.NonNull"),
    FqName("android.annotation.NonNull"),
    FqName("com.android.annotations.NonNull"),
    FqName("org.eclipse.jdt.annotation.NonNull"),
    FqName("org.checkerframework.checker.nullness.qual.NonNull"),
    FqName("lombok.NonNull"),
    FqName("io.reactivex.annotations.NonNull")
)

val COMPATQUAL_NULLABLE_ANNOTATION = FqName("org.checkerframework.checker.nullness.compatqual.NullableDecl")
val COMPATQUAL_NONNULL_ANNOTATION = FqName("org.checkerframework.checker.nullness.compatqual.NonNullDecl")

val ANDROIDX_RECENTLY_NULLABLE_ANNOTATION = FqName("androidx.annotation.RecentlyNullable")
val ANDROIDX_RECENTLY_NON_NULL_ANNOTATION = FqName("androidx.annotation.RecentlyNonNull")

val NULLABILITY_ANNOTATIONS = mutableSetOf<FqName>() +
        NULLABLE_ANNOTATIONS +
        JAVAX_NONNULL_ANNOTATION +
        NOT_NULL_ANNOTATIONS +
        COMPATQUAL_NULLABLE_ANNOTATION +
        COMPATQUAL_NONNULL_ANNOTATION +
        ANDROIDX_RECENTLY_NULLABLE_ANNOTATION +
        ANDROIDX_RECENTLY_NON_NULL_ANNOTATION +
        JSPECIFY_NULLABLE +
        JSPECIFY_NOT_NULL +
        JSPECIFY_NULLNESS_UNKNOWN +
        JSPECIFY_DEFAULT_NULLABLE +
        JSPECIFY_DEFAULT_NOT_NULL +
        JSPECIFY_DEFAULT_NULLNESS_UNKNOWN

val READ_ONLY_ANNOTATIONS = listOf(
    JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION,
    JvmAnnotationNames.READONLY_ANNOTATION
)

val MUTABLE_ANNOTATIONS = listOf(
    JvmAnnotationNames.JETBRAINS_MUTABLE_ANNOTATION,
    JvmAnnotationNames.MUTABLE_ANNOTATION
)

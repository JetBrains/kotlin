/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

@Suppress("unused")
// It should only have major (X.Y.0) and minor (X.Y.20) releases. We don't care about bug-fix ones.
enum class CompilerVersionOfApiDeprecation {
    _2_0_0,
    _2_0_20,
    _2_1_0,
    _2_1_20,
}

/**
 * TL;DL:
 * @DeprecatedCompilerApi - IDE-only warning for us, compiler warning for users (in future).
 * @DeprecatedForRemovalCompilerApi - compiler error for us and for users, may @OptIn though.
 */

/**
 * An API that we would like to migrate away from in the compiler, and eventually drop.
 * It produces IDE-only warning.
 *
 * Note: We don't simply use @Deprecated annotation because of how our build
 * and infrastructure are configured.
 */
@Target(
    AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS,
    AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD,
)
annotation class DeprecatedCompilerApi(
    val deprecatedSince: CompilerVersionOfApiDeprecation,
    val message: String = "",
    val replaceWith: String = "",
)

/**
 * An API that we essentially dropped, but temporarily keep for source or binary compatibility with some
 * third-party - usually compiler plugins in User Projects that we test against.
 *
 * Note: We don't simply use @Deprecated annotation because of how our build
 * and infrastructure are configured.
 */
@Target(
    AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD,
)
@RequiresOptIn("This compiler API is deprecated and will be removed soon.")
annotation class DeprecatedForRemovalCompilerApi(
    val deprecatedSince: CompilerVersionOfApiDeprecation,
    val message: String = "",
    val replaceWith: String = "",
)

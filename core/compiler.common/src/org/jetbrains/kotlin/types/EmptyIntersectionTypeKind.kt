/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

enum class EmptyIntersectionTypeKind(val description: String) {
    MULTIPLE_CLASSES("multiple incompatible classes"),
    INCOMPATIBLE_SUPERTYPES("incompatible supertypes"),
    INCOMPATIBLE_TYPE_ARGUMENTS("incompatible type arguments"),
    SINGLE_FINAL_CLASS("final class and interface")
}

fun EmptyIntersectionTypeKind.isDefinitelyEmpty(): Boolean =
    this == EmptyIntersectionTypeKind.MULTIPLE_CLASSES
            || this == EmptyIntersectionTypeKind.INCOMPATIBLE_SUPERTYPES
            || this == EmptyIntersectionTypeKind.INCOMPATIBLE_TYPE_ARGUMENTS

fun EmptyIntersectionTypeKind.isPossiblyEmpty(): Boolean = this == EmptyIntersectionTypeKind.SINGLE_FINAL_CLASS

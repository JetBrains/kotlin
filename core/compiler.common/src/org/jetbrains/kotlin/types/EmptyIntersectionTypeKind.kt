/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

enum class EmptyIntersectionTypeKind(val description: String, val isDefinitelyEmpty: Boolean) {
    MULTIPLE_CLASSES("multiple incompatible classes", isDefinitelyEmpty = true),
    FINAL_CLASS_AND_INTERFACE("final class and interface", isDefinitelyEmpty = false)
}

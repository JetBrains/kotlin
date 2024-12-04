/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

enum class ValueKind {
    GENERAL,
    DEFAULT_PARAMETER,
    DEFAULT_INLINE_PARAMETER,
    DEFAULT_MASK,
    METHOD_HANDLE_IN_DEFAULT,
    READ_OF_INLINE_LAMBDA_FOR_INLINE_SUSPEND_PARAMETER,
    READ_OF_OBJECT_FOR_INLINE_SUSPEND_PARAMETER
}

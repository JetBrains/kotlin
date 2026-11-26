/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

enum class DeclarationStatusModifier(val mask: Int) {
    EXPECT(0x1),
    ACTUAL(0x2),
    OVERRIDE(0x4),
    OPERATOR(0x8),
    INFIX(0x10),
    INLINE(0x20),
    TAILREC(0x40),
    EXTERNAL(0x80),
    CONST(0x100),
    LATEINIT(0x200),
    INNER(0x400),
    COMPANION(0x800),
    DATA(0x1000),
    SUSPEND(0x2000),
    STATIC(0x4000),
    FROM_SEALED(0x8000),
    FROM_ENUM(0x10000),
    FUN(0x20000),
    HAS_STABLE_PARAMETER_NAMES(0x40000),
    VALUE(0x80000),
    HAS_MUST_USE_RETURN_VALUE(0x100000),
    HAS_IGNORABLE_RETURN_VALUE(0x200000),
    RICH_ERROR(0x400000)
    ;
}

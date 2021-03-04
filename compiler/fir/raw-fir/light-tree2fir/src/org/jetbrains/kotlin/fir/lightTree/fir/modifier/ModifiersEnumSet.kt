/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

enum class ClassModifier {
    ENUM,
    ANNOTATION,
    DATA,
    INLINE,
    INNER,
    COMPANION,
    FUN
}

enum class MemberModifier {
    OVERRIDE,
    LATEINIT
}

enum class VisibilityModifier {
    PUBLIC,
    PRIVATE,
    INTERNAL,
    PROTECTED,
    UNKNOWN
}

enum class FunctionModifier {
    TAILREC,
    OPERATOR,
    INFIX,
    INLINE,
    EXTERNAL,
    SUSPEND
}

enum class PropertyModifier {
    CONST
}

enum class InheritanceModifier {
    ABSTRACT,
    FINAL,
    OPEN,
    SEALED
}

enum class ParameterModifier {
    VARARG,
    NOINLINE,
    CROSSINLINE,
    CONST
}

enum class PlatformModifier {
    EXPECT,
    ACTUAL,
    HEADER,
    IMPL
}

enum class VarianceModifier {
    IN,
    OUT,
    INVARIANT
}

enum class ReificationModifier {
    REIFIED
}
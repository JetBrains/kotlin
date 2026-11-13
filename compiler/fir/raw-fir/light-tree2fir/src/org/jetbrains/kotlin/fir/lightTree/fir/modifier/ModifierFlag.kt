/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

enum class ModifierFlag(val value: Long) {
    NONE(0L),
    CLASS_ENUM(1L shl 0),
    CLASS_ANNOTATION(1L shl 1),
    CLASS_DATA(1L shl 2),
    CLASS_INLINE(1L shl 3),
    CLASS_INNER(1L shl 4),
    CLASS_COMPANION(1L shl 5),
    CLASS_FUN(1L shl 6),
    MEMBER_OVERRIDE(1L shl 7),
    MEMBER_LATEINIT(1L shl 8),
    VISIBILITY_PUBLIC(1L shl 9),
    VISIBILITY_PRIVATE(1L shl 10),
    VISIBILITY_INTERNAL(1L shl 11),
    VISIBILITY_PROTECTED(1L shl 12),
    VISIBILITY_UNKNOWN(1L shl 13),
    FUNCTION_TAILREC(1L shl 14),
    FUNCTION_OPERATOR(1L shl 15),
    FUNCTION_INFIX(1L shl 16),
    FUNCTION_INLINE(1L shl 17),
    FUNCTION_EXTERNAL(1L shl 18),
    FUNCTION_SUSPEND(1L shl 19),
    PROPERTY_CONST(1L shl 20),
    INHERITANCE_ABSTRACT(1L shl 21),
    INHERITANCE_FINAL(1L shl 22),
    INHERITANCE_OPEN(1L shl 23),
    INHERITANCE_SEALED(1L shl 24),
    PARAMETER_VARARG(1L shl 25),
    PARAMETER_NOINLINE(1L shl 26),
    PARAMETER_CROSSINLINE(1L shl 27),
    PARAMETER_CONST(1L shl 28),
    PLATFORM_EXPECT(1L shl 29),
    PLATFORM_ACTUAL(1L shl 30),
    VARIANCE_IN(1L shl 31),
    VARIANCE_OUT(1L shl 32),
    VARIANCE_INVARIANT(1L shl 33),
    REIFICATION_REIFIED(1L shl 34),
    CLASS_VALUE(1L shl 35),
    CLASS_ERROR(1L shl 36),
    ;

    companion object {
        val ElementTypeToModifierFlagMap: Map<Int, ModifierFlag> = hashMapOf(
            // Class
            KotlinSyntaxTokenIds.ENUM_MODIFIER to CLASS_ENUM,
            KotlinSyntaxTokenIds.ANNOTATION_MODIFIER to CLASS_ANNOTATION,
            KotlinSyntaxTokenIds.DATA_MODIFIER to CLASS_DATA,
            KotlinSyntaxTokenIds.INNER_MODIFIER to CLASS_INNER,
            KotlinSyntaxTokenIds.COMPANION_MODIFIER to CLASS_COMPANION,
            KotlinSyntaxTokenIds.FUN_MODIFIER to CLASS_FUN,
            KotlinSyntaxTokenIds.ERROR_MODIFIER to CLASS_ERROR,

            // Member
            KotlinSyntaxTokenIds.OVERRIDE_MODIFIER to MEMBER_OVERRIDE,
            KotlinSyntaxTokenIds.LATEINIT_MODIFIER to MEMBER_LATEINIT,

            // Visibility
            KotlinSyntaxTokenIds.PUBLIC_MODIFIER to VISIBILITY_PUBLIC,
            KotlinSyntaxTokenIds.PRIVATE_MODIFIER to VISIBILITY_PRIVATE,
            KotlinSyntaxTokenIds.INTERNAL_MODIFIER to VISIBILITY_INTERNAL,
            KotlinSyntaxTokenIds.PROTECTED_MODIFIER to VISIBILITY_PROTECTED,

            // Function
            KotlinSyntaxTokenIds.TAILREC_MODIFIER to FUNCTION_TAILREC,
            KotlinSyntaxTokenIds.OPERATOR_MODIFIER to FUNCTION_OPERATOR,
            KotlinSyntaxTokenIds.INFIX_MODIFIER to FUNCTION_INFIX,
            KotlinSyntaxTokenIds.EXTERNAL_MODIFIER to FUNCTION_EXTERNAL,
            KotlinSyntaxTokenIds.SUSPEND_MODIFIER to FUNCTION_SUSPEND,

            // Inheritance
            KotlinSyntaxTokenIds.ABSTRACT_MODIFIER to INHERITANCE_ABSTRACT,
            KotlinSyntaxTokenIds.FINAL_MODIFIER to INHERITANCE_FINAL,
            KotlinSyntaxTokenIds.OPEN_MODIFIER to INHERITANCE_OPEN,
            KotlinSyntaxTokenIds.SEALED_MODIFIER to INHERITANCE_SEALED,

            // Parameter
            KotlinSyntaxTokenIds.VARARG_MODIFIER to PARAMETER_VARARG,
            KotlinSyntaxTokenIds.NOINLINE_MODIFIER to PARAMETER_NOINLINE,
            KotlinSyntaxTokenIds.CROSSINLINE_MODIFIER to PARAMETER_CROSSINLINE,

            // Platform
            KotlinSyntaxTokenIds.EXPECT_MODIFIER to PLATFORM_EXPECT,
            KotlinSyntaxTokenIds.ACTUAL_MODIFIER to PLATFORM_ACTUAL,

            // Variance
            KotlinSyntaxTokenIds.IN_MODIFIER to VARIANCE_IN,
            KotlinSyntaxTokenIds.OUT_MODIFIER to VARIANCE_OUT,

            // Reification
            KotlinSyntaxTokenIds.REIFIED_MODIFIER to REIFICATION_REIFIED,
        )
    }
}

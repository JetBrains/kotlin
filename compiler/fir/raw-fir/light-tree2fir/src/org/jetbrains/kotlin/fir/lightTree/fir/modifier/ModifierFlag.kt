/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens

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
    STATIC(1L shl 36);

    companion object {
        val ElementTypeToModifierFlagMap: Map<IElementType, ModifierFlag> = mutableMapOf(
            // Class
            KtTokens.ENUM_KEYWORD to CLASS_ENUM,
            KtTokens.ANNOTATION_KEYWORD to CLASS_ANNOTATION,
            KtTokens.DATA_KEYWORD to CLASS_DATA,
            KtTokens.INNER_KEYWORD to CLASS_INNER,
            KtTokens.COMPANION_KEYWORD to CLASS_COMPANION,
            KtTokens.FUN_KEYWORD to CLASS_FUN,

            // Member
            KtTokens.OVERRIDE_KEYWORD to MEMBER_OVERRIDE,
            KtTokens.LATEINIT_KEYWORD to MEMBER_LATEINIT,

            // Visibility
            KtTokens.PUBLIC_KEYWORD to VISIBILITY_PUBLIC,
            KtTokens.PRIVATE_KEYWORD to VISIBILITY_PRIVATE,
            KtTokens.INTERNAL_KEYWORD to VISIBILITY_INTERNAL,
            KtTokens.PROTECTED_KEYWORD to VISIBILITY_PROTECTED,

            // Function
            KtTokens.TAILREC_KEYWORD to FUNCTION_TAILREC,
            KtTokens.OPERATOR_KEYWORD to FUNCTION_OPERATOR,
            KtTokens.INFIX_KEYWORD to FUNCTION_INFIX,
            KtTokens.EXTERNAL_KEYWORD to FUNCTION_EXTERNAL,
            KtTokens.SUSPEND_KEYWORD to FUNCTION_SUSPEND,

            // Inheritance
            KtTokens.ABSTRACT_KEYWORD to INHERITANCE_ABSTRACT,
            KtTokens.FINAL_KEYWORD to INHERITANCE_FINAL,
            KtTokens.OPEN_KEYWORD to INHERITANCE_OPEN,
            KtTokens.SEALED_KEYWORD to INHERITANCE_SEALED,

            // Parameter
            KtTokens.VARARG_KEYWORD to PARAMETER_VARARG,
            KtTokens.NOINLINE_KEYWORD to PARAMETER_NOINLINE,
            KtTokens.CROSSINLINE_KEYWORD to PARAMETER_CROSSINLINE,

            // Platform
            KtTokens.EXPECT_KEYWORD to PLATFORM_EXPECT,
            KtTokens.ACTUAL_KEYWORD to PLATFORM_ACTUAL,

            // Variance
            KtTokens.IN_KEYWORD to VARIANCE_IN,
            KtTokens.OUT_KEYWORD to VARIANCE_OUT,

            // Reification
            KtTokens.REIFIED_KEYWORD to REIFICATION_REIFIED,

            // Static
            KtTokens.STATIC_KEYWORD to STATIC,
        )
    }
}
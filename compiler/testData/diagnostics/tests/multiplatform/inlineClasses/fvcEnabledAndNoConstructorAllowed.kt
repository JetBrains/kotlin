// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses +AllowExpectValueClassesWithNoPrimaryConstructor
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT
// WITH_STDLIB

// MODULE: common
// FILE: exp.kt
package p

import kotlin.jvm.JvmInline

expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_JvmInline_value
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline_value
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_value
expect @JvmInline value class E_JvmInline_value_A_inline
expect @JvmInline value class E_JvmInline_value_A_JvmInline_value
expect @JvmInline value class E_JvmInline_value_A_inline_value
expect @JvmInline value class E_JvmInline_value_A_value
expect inline value class E_inline_value_A_inline
expect inline value class E_inline_value_A_JvmInline_value
expect inline value class E_inline_value_A_inline_value
expect inline value class E_inline_value_A_value
expect value class E_value_A_inline
expect value class E_value_A_JvmInline_value
expect value class E_value_A_inline_value
expect value class E_value_A_value

// MODULE: jvm()()(common)
// FILE: act.kt
package p

actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline(val x: Int)
actual @JvmInline value class E_inline_A_JvmInline_value(val x: Int)
actual inline value class E_inline_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_inline_A_value<!>(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_JvmInline_value_A_inline(val x: Int)
actual @JvmInline value class E_JvmInline_value_A_JvmInline_value(val x: Int)
actual inline value class E_JvmInline_value_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_JvmInline_value_A_value<!>(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_value_A_inline(val x: Int)
actual @JvmInline value class E_inline_value_A_JvmInline_value(val x: Int)
actual inline value class E_inline_value_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_inline_value_A_value<!>(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_value_A_inline<!>(val x: Int)
actual @JvmInline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_value_A_JvmInline_value<!>(val x: Int)
actual inline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>E_value_A_inline_value<!>(val x: Int)
actual value class E_value_A_value(val x: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, value */

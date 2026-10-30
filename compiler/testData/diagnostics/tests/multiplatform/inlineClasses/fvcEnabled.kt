// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

// MODULE: common
// FILE: exp.kt
package p

import kotlin.jvm.JvmInline

expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline(val x: Int)
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_JvmInline_value(val x: Int)
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline_value(val x: Int)
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_value(val x: Int)
expect @JvmInline value class E_JvmInline_value_A_inline(val x: Int)
expect @JvmInline value class E_JvmInline_value_A_JvmInline_value(val x: Int)
expect @JvmInline value class E_JvmInline_value_A_inline_value(val x: Int)
expect @JvmInline value class E_JvmInline_value_A_value(val x: Int)
expect inline value class E_inline_value_A_inline(val x: Int)
expect inline value class E_inline_value_A_JvmInline_value(val x: Int)
expect inline value class E_inline_value_A_inline_value(val x: Int)
expect inline value class E_inline_value_A_value(val x: Int)
expect value class E_value_A_inline(val x: Int)
expect value class E_value_A_JvmInline_value(val x: Int)
expect value class E_value_A_inline_value(val x: Int)
expect value class E_value_A_value(val x: Int)

// MODULE: jvm()()(common)
// FILE: act.kt
package p

actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_A_inline(val x: Int)
actual @JvmInline value class E_inline_A_JvmInline_value(val x: Int)
actual inline value class E_inline_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect inline value class E_inline_A_value : Any; public final actual value class E_inline_A_value : Any; the modifiers are different (companion, inner, inline, value)")!>E_inline_A_value<!> actual constructor(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_JvmInline_value_A_inline(val x: Int)
actual @JvmInline value class E_JvmInline_value_A_JvmInline_value(val x: Int)
actual inline value class E_JvmInline_value_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect inline value class E_JvmInline_value_A_value : Any; public final actual value class E_JvmInline_value_A_value : Any; the modifiers are different (companion, inner, inline, value)")!>E_JvmInline_value_A_value<!> actual constructor(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class E_inline_value_A_inline(val x: Int)
actual @JvmInline value class E_inline_value_A_JvmInline_value(val x: Int)
actual inline value class E_inline_value_A_inline_value(val x: Int)
actual value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect inline value class E_inline_value_A_value : Any; public final actual value class E_inline_value_A_value : Any; the modifiers are different (companion, inner, inline, value)")!>E_inline_value_A_value<!> actual constructor(val x: Int)
actual <!INLINE_CLASS_DEPRECATED!>inline<!> class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect value class E_value_A_inline : Any; public final actual inline value class E_value_A_inline : Any; the modifiers are different (companion, inner, inline, value)")!>E_value_A_inline<!>(val x: Int)
actual @JvmInline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect value class E_value_A_JvmInline_value : Any; public final actual inline value class E_value_A_JvmInline_value : Any; the modifiers are different (companion, inner, inline, value)")!>E_value_A_JvmInline_value<!>(val x: Int)
actual inline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS("public final expect value class E_value_A_inline_value : Any; public final actual inline value class E_value_A_inline_value : Any; the modifiers are different (companion, inner, inline, value)")!>E_value_A_inline_value<!>(val x: Int)
actual value class E_value_A_value actual constructor(val x: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, value */

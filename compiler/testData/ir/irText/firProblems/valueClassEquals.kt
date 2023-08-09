// SKIP_KLIB_TEST
// IGNORE_BACKEND: JS_IR
// WITH_STDLIB
// LANGUAGE: +ValueClasses

// KT-61141: OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE: Declaration annotated with '@OptionalExpectation' can only be used in common module sources (6,19) in /valueClassEquals.kt
// IGNORE_BACKEND: NATIVE

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals

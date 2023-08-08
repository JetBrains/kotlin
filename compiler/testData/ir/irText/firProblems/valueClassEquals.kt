// SKIP_KLIB_TEST
// IGNORE_BACKEND: JS_IR
// WITH_STDLIB
// LANGUAGE: +ValueClasses

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals

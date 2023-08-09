// SKIP_KLIB_TEST
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +ValueClasses

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals

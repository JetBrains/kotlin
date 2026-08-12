// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSameActual
// ISSUE: KT-69909
// WITH_STDLIB
// IGNORE_IR_DESERIALIZATION_TEST: JVM_IR

// MODULE: common
// FILE: common.kt

expect class A
expect class B

expect fun foo(it: A): String
expect fun foo(it: B): String

// MODULE: user()()(common)
// FILE: common.kt

fun bar(a: A, b: B) = foo(a) + foo(b)

// MODULE: platform()()(common, user)
// FILE: platform.kt

actual typealias A = Int
actual typealias B = Int

actual fun foo(it: Int): String = "!".repeat(it)

fun box() = when (val that = bar(1, 2)) {
    "!!!" -> "OK"
    else -> "FAIL: $that"
}

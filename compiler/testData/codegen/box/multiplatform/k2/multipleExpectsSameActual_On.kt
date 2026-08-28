// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307
// WITH_STDLIB
// IGNORE_IR_DESERIALIZATION_TEST: JVM_IR

// MODULE: common

expect class A
expect class B

expect fun foo(it: A): String
expect fun foo(it: B): String

// MODULE: intermediate()()(common)

fun bar(a: A, b: B) = foo(a) + foo(b)

// MODULE: platform()()(intermediate)

actual typealias A = Int
actual typealias B = Int

actual fun foo(it: Int): String = "!".repeat(it)

fun box() = when (val that = bar(1, 2)) {
    "!!!" -> "OK"
    else -> "FAIL: $that"
}

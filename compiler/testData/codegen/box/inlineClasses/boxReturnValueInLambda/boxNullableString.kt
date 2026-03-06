// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: String?)

fun useX(x: X): String = x.x ?: "fail: $x"

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X("OK") })
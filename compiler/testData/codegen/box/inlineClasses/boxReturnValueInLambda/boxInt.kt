// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Int)

fun useX(x: X): String = if (x.x == 42) "OK" else "fail: $x"

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X(42) })
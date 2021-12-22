// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: Int>(val x: T)

fun useX(x: X<Int>): String = if (x.x == 42) "OK" else "fail: $x"

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X(42) })
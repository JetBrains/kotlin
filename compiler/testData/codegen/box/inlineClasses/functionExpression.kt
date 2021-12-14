// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

inline fun new(init: (Z) -> Unit): Z = Z(42)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val value: Int)

fun box(): String =
    if (new(fun(z: Z) {}).value == 42) "OK" else "Fail"

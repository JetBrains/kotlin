// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

// FILE: lib.kt
inline fun new(init: (Z) -> Unit): Z = Z(42)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val value: Int)

// FILE: main.kt
fun box(): String =
    if (new(fun(z: Z) {}).value == 42) "OK" else "Fail"

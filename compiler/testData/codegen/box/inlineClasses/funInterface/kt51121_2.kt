// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// IGNORE_BACKEND: JVM
// FILE: 1.kt

val f: F = F { value -> Z(value) }

fun box(): String =
    f.foo("OK").value

// FILE: 2.kt

fun interface F {
    fun foo(s: String): Z
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val value: String)


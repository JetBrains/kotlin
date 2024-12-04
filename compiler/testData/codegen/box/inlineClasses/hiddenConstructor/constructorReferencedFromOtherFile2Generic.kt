// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: 2.kt

fun box(): String = X(Z("OK")).z.result

// FILE: 1.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: String>(val result: T)

class X(val z: Z<String>)

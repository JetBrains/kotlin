// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// FILE: 1.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val x: T)

fun accessProperty(y: B): A<String> {
    y.a = A("OK")
    return y.a
}

// FILE: 2.kt

class B(var a: A<String>)

fun box(): String = accessProperty(B(A("Fail"))).x

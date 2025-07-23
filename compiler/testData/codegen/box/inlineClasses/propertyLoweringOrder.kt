// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// SKIP_SPLITTING_TO_TWO_MODULES: ANY
// FILE: 1.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: String)

fun accessProperty(y: B): A {
    y.a = A("OK")
    return y.a
}

// FILE: 2.kt

class B(var a: A)

fun box(): String = accessProperty(B(A("Fail"))).x

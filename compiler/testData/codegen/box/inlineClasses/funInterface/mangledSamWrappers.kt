// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val value: String)

fun interface B {
    fun f(x: A): A
}

inline fun g(unit: Unit = Unit, b: B): A {
    return b.f(A("Fail"))
}

// FILE: main.kt
fun box(): String {
    val b = { _ : A -> A("O") }
    return g(b = b).value + g { A("K") }.value
}

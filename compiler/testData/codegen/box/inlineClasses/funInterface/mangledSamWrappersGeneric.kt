// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val value: T)

fun interface B {
    fun f(x: A<String>): A<String>
}

inline fun g(unit: Unit = Unit, b: B): A<String> {
    return b.f(A("Fail"))
}

// FILE: main.kt
fun box(): String {
    val b = { _ : A<*> -> A("O") }
    return g(b = b).value + g { A("K") }.value
}

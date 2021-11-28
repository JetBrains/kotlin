// IGNORE_BACKEND: JVM
// WITH_STDLIB
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val value: String)

fun interface B {
    fun f(x: A): A
}

inline fun g(unit: Unit = Unit, b: B): A {
    return b.f(A("Fail"))
}

fun box(): String {
    val b = { _ : A -> A("O") }
    return g(b = b).value + g { A("K") }.value
}

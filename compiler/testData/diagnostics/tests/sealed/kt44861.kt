// FIR_IDENTICAL
// ISSUE: KT-44861
// DIAGNOSTICS: -UNUSED_VARIABLE

sealed class Foo() {
    class A : Foo()
    class B : Foo()
}

fun Foo(kind: String = "A"): Foo = when (kind) {
    "A" -> Foo.A()
    "B" -> Foo.B()
    else -> throw Exception()
}

fun main() {
    val foo = Foo()
}

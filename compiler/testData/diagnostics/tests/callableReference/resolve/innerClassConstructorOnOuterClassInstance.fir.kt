// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

abstract class A {
    inner class InnerInA
}

abstract class B : A()

fun foo(a: A) {
    if (a is B) {
        val v = a::InnerInA
    }
}
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A {
    fun foo() = 42

    object B: A()
}

fun test() {
    (A::foo)(A.B)
}

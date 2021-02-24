// FIR_IDENTICAL
data class A(val component1: Int)

fun foo(a: A) {
    a.component1()
    a.component1
}

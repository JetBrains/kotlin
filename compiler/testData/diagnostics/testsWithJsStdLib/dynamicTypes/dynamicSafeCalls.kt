// FIR_IDENTICAL
fun test(d: dynamic) {
    val v1 = d?.foo()
    v1.isDynamic() // to check that anything is resolvable

    val v2 = d!!.foo(1)
    v2.isDynamic() // to check that anything is resolvable
}
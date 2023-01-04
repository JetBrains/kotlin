// FIR_IDENTICAL
fun <!DYNAMIC_RECEIVER_NOT_ALLOWED!>dynamic<!>.test() {
    val v1 = foo()
    v1.isDynamic() // to check that anything is resolvable

    val v2 = foo(1)
    v2.isDynamic() // to check that anything is resolvable

    val v3 = foo(1, "")
    v3.isDynamic() // to check that anything is resolvable

    val v4 = foo<String>()
    v4.isDynamic() // to check that anything is resolvable

    val v5 = foo
    v5.isDynamic() // to check that anything is resolvable

    foo = 1
}

class C {
    fun foo() {}
}

fun test(a: C?, nn: () -> Nothing?) {
    a ?: nn()
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>()

    a ?: return
    a.foo()
}

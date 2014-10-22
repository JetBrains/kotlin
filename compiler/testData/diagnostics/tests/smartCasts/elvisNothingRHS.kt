class C {
    fun foo() {}
}

fun test(a: C?, nn: () -> Nothing?) {
    a ?: nn()
    a<!UNSAFE_CALL!>.<!>foo()

    a ?: return
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
}

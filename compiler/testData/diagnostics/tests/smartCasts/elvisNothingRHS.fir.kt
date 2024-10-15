// RUN_PIPELINE_TILL: FRONTEND
class C {
    fun foo() {}
}

fun test(a: C?, nn: () -> Nothing?) {
    a ?: nn()
    a<!UNSAFE_CALL!>.<!>foo()

    a ?: return
    a.foo()
}

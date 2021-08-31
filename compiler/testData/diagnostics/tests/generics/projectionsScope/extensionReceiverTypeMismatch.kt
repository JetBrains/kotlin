// FIR_IDENTICAL
class A<T> {
    fun T.foo() {}
    fun Out<T>.bar() {}
}
class Out<out E>

fun test(x: A<out CharSequence>, y: Out<CharSequence>) {
    with(x) {
        // TODO: this diagnostic could be replaced with TYPE_MISMATCH_DUE_TO_TYPE_PROJECTION
        "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
        y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()

        with(y) {
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
        }
    }
}

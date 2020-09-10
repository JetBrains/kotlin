// !WITH_NEW_INFERENCE
class A<T> {
    fun T.foo() {}
    fun Out<T>.bar() {}
}
class Out<out E>

fun test(x: A<out CharSequence>, y: Out<CharSequence>) {
    with(x) {
        // TODO: this diagnostic could be replaced with TYPE_MISMATCH_DUE_TO_TYPE_PROJECTION
        "".<!INAPPLICABLE_CANDIDATE!>foo<!>()
        y.<!INAPPLICABLE_CANDIDATE!>bar<!>()

        with(y) {
            <!INAPPLICABLE_CANDIDATE!>bar<!>()
        }
    }
}

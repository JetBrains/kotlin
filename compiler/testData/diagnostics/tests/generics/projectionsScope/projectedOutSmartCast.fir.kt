// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.<!INAPPLICABLE_CANDIDATE!>bindTo<!>("")

    if (x is C<*>) {
        x.<!INAPPLICABLE_CANDIDATE!>bindTo<!>("")
        with(x) {
            <!INAPPLICABLE_CANDIDATE!>bindTo<!>("")
        }
    }

    with(x) {
        if (this is C<*>) {
            <!INAPPLICABLE_CANDIDATE!>bindTo<!>("")
        }
    }
}

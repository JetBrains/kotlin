// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.<!MEMBER_PROJECTED_OUT!>bindTo<!>("")

    if (x is C<*>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!MEMBER_PROJECTED_OUT!>bindTo<!>("")
        with(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            <!MEMBER_PROJECTED_OUT!>bindTo<!>("")
        }
    }

    with(x) {
        if (this is C<*>) {
            <!MEMBER_PROJECTED_OUT, DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>bindTo<!>("")
        }
    }
}

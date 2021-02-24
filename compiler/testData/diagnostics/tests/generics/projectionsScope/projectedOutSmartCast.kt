// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.<!MEMBER_PROJECTED_OUT{OI}!>bindTo<!>(<!TYPE_MISMATCH{NI}!>""<!>)

    if (x is C<*>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!MEMBER_PROJECTED_OUT{OI}!>bindTo<!>(<!TYPE_MISMATCH{NI}!>""<!>)
        with(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            <!MEMBER_PROJECTED_OUT{OI}!>bindTo<!>(<!TYPE_MISMATCH{NI}!>""<!>)
        }
    }

    with(x) {
        if (this is C<*>) {
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST, MEMBER_PROJECTED_OUT{OI}!>bindTo<!>(<!TYPE_MISMATCH{NI}!>""<!>)
        }
    }
}

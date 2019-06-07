// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.<!OI;MEMBER_PROJECTED_OUT!>bindTo<!>(<!NI;TYPE_MISMATCH!>""<!>)

    if (x is C<*>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!OI;MEMBER_PROJECTED_OUT!>bindTo<!>(<!NI;TYPE_MISMATCH!>""<!>)
        with(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            <!OI;MEMBER_PROJECTED_OUT!>bindTo<!>(<!NI;TYPE_MISMATCH!>""<!>)
        }
    }

    with(x) {
        if (this is C<*>) {
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST, OI;MEMBER_PROJECTED_OUT!>bindTo<!>(<!NI;TYPE_MISMATCH!>""<!>)
        }
    }
}

// DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.bindTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)

    if (x is C<*>) {
        x.bindTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
        with(x) {
            bindTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }
    }

    with(x) {
        if (this is C<*>) {
            bindTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }
    }
}

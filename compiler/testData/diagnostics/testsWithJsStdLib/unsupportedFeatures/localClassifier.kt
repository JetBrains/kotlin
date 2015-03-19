// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!NON_TOPLEVEL_CLASS_DECLARATION!>class A<!> {
        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class E<!> {
            <!NESTED_CLASS_NOT_ALLOWED, NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!>
        }
    }

    <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
    }
}

fun foo() {
    <!NON_TOPLEVEL_CLASS_DECLARATION!>class A<!> {
        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class E<!> {
            <!NESTED_CLASS_NOT_ALLOWED, NON_TOPLEVEL_CLASS_DECLARATION!>interface T<!>
        }
    }

    <!LOCAL_INTERFACE_NOT_ALLOWED, NON_TOPLEVEL_CLASS_DECLARATION!>interface T<!> {
    }

    object {
        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>
    }
}

class A {
    <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class C<!>

        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class D<!>

        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
            <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
            <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>
        }

        companion object {}
    }

    inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class I<!>

    <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>

        companion object {}
    }

    <!NON_TOPLEVEL_CLASS_DECLARATION!>enum class E<!> {
        X Y

        companion object {}
    }

    companion object {}
}

trait T {
    <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class C<!>

        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class D<!>

        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
            <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
            <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>
        }

        companion object {}
    }

    <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>

        companion object {}
    }

    <!NON_TOPLEVEL_CLASS_DECLARATION!>enum class E<!> {
        X Y

        companion object {}
    }

    companion object {}
}

enum class E {
    X Y

    <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class C<!>

        inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class D<!>

        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
            <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
            <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>
        }

        companion object {}
    }

    inner <!NON_TOPLEVEL_CLASS_DECLARATION!>class I<!>

    <!NON_TOPLEVEL_CLASS_DECLARATION!>trait T<!> {
        <!NON_TOPLEVEL_CLASS_DECLARATION!>trait A<!>
        <!NON_TOPLEVEL_CLASS_DECLARATION!>class B<!>

        companion object {}
    }

    <!NON_TOPLEVEL_CLASS_DECLARATION!>enum class E<!> {
        X Y

        companion object {}
    }

    companion object {}
}

// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
<!NOTHING_TO_INLINE!>inline<!> fun foo1(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>x: suspend () -> Unit<!>) {}
<!NOTHING_TO_INLINE!>inline<!> fun foo2(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>crossinline x: suspend () -> Unit<!>) {}
<!NOTHING_TO_INLINE!>inline<!> fun foo3(noinline x: suspend () -> Unit) {}
<!NOTHING_TO_INLINE!>inline<!> fun foo4(<!INCOMPATIBLE_MODIFIERS!>noinline<!> <!INCOMPATIBLE_MODIFIERS!>crossinline<!> x: suspend () -> Unit) {}

suspend <!NOTHING_TO_INLINE!>inline<!> fun bar1(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>x: suspend () -> Unit<!>) {}
suspend <!NOTHING_TO_INLINE!>inline<!> fun bar2(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>crossinline x: suspend () -> Unit<!>) {}
suspend <!NOTHING_TO_INLINE!>inline<!> fun bar3(noinline x: suspend () -> Unit) {}
suspend <!NOTHING_TO_INLINE!>inline<!> fun bar4(<!INCOMPATIBLE_MODIFIERS!>noinline<!> <!INCOMPATIBLE_MODIFIERS!>crossinline<!> x: suspend () -> Unit) {}

suspend fun baz() {
    foo1 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    foo2 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    foo3 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    foo4 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    bar1 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    bar2 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    bar3 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }

    bar4 {
        <!RETURN_NOT_ALLOWED!>return@baz<!>
    }
}

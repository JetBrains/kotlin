// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
inline fun foo1(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>x: suspend () -> Unit<!>) {}
inline fun foo2(crossinline x: suspend () -> Unit) {}
<!NOTHING_TO_INLINE!>inline<!> fun foo3(noinline x: suspend () -> Unit) {}
<!NOTHING_TO_INLINE!>inline<!> fun foo4(<!INCOMPATIBLE_MODIFIERS!>noinline<!> <!INCOMPATIBLE_MODIFIERS!>crossinline<!> x: suspend () -> Unit) {}

suspend inline fun bar1(x: <!REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE!>suspend<!> () -> Unit) {}
suspend inline fun bar2(crossinline x: suspend () -> Unit) {}
suspend inline fun bar3(noinline x: suspend () -> Unit) {}
suspend inline fun bar4(<!INCOMPATIBLE_MODIFIERS!>noinline<!> <!INCOMPATIBLE_MODIFIERS!>crossinline<!> x: suspend () -> Unit) {}

suspend fun baz() {
    foo1 {
        return@baz
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
        return@baz
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

fun f() {
    foo {
        bar {
            <!RETURN_NOT_ALLOWED!>return@foo<!> 1
        }
        return@foo 1
    }
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}
fun bar(<!UNUSED_PARAMETER!>a<!>: Any) {}
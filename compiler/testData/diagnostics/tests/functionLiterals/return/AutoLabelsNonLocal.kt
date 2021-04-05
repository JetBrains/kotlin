// FIR_IDENTICAL
fun f() {
    foo {
        bar {
            <!RETURN_NOT_ALLOWED!>return@foo<!> 1
        }
        return@foo 1
    }
}

fun foo(a: Any) {}
fun bar(a: Any) {}

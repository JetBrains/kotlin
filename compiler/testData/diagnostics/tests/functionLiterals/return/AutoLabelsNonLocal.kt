fun f() {
    foo {(): Int ->
        bar {
            (): Int ->
            // The actual error message should be "return not allowed",
            // but due to a bug in label resolution it is "unresolved reference"
            return<!UNRESOLVED_REFERENCE!>@foo<!> 1
        }
        return@foo 1
    }
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}
fun bar(<!UNUSED_PARAMETER!>a<!>: Any) {}
suspend fun foo() {}

fun noSuspend() {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()
}

class A {
    init {
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()
    }
}

val x = <!ILLEGAL_SUSPEND_FUNCTION_CALL!>foo<!>()

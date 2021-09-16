// !LANGUAGE: +ContextReceivers

interface A {
    fun f() {}
}
class B : A

fun testLocalFunction() {
    context(A)
    fun local() {
        <!UNRESOLVED_REFERENCE!>f<!>()
    }
    with(B()) {
        local()
    }
    local()
}

fun testLocalClass() {
    context(A)
    class Local {
        fun local() {
            <!UNRESOLVED_REFERENCE!>f<!>()
        }
    }
    with(B()) {
        Local().local()
    }
    Local()
}
// !LANGUAGE: +ContextReceivers

interface A {
    fun f() {}
}
class B : A

fun testLocalFunction() {
    context(A)
    fun local() {
        f()
    }
    with(B()) {
        local()
    }
    <!NO_CONTEXT_RECEIVER!>local()<!>
}

fun testLocalClass() {
    context(A)
    class Local {
        fun local() {
            f()
        }
    }
    with(B()) {
        Local().local()
    }
    <!NO_CONTEXT_RECEIVER!>Local()<!>
}
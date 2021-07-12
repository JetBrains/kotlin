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
    local()
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
    Local()
}
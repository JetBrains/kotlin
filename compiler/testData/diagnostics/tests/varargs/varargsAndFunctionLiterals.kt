fun f(vararg <!UNUSED_PARAMETER!>t<!> : Int, <!UNUSED_PARAMETER!>f<!> : ()->Unit) {
}

fun test() {
    f {}
    f() {}
    f(1) {

    }
    f(1, 2) {

    }
}
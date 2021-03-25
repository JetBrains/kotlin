// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun f(vararg t : Int, f : ()->Unit) {
}

fun test() {
    f {}
    f() {}
    f(1) {

    }
    f(1, 2) {

    }
}
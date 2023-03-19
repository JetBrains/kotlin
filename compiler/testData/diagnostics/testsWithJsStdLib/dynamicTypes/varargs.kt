// FIR_IDENTICAL
// !MARK_DYNAMIC_CALLS

fun test() {
    v()
    v(1)
    v(1, "")
}

fun v(vararg d: dynamic) {
    for (dd in d) {
        dd.<!DEBUG_INFO_DYNAMIC!>foo<!>()
    }
}
// !MARK_DYNAMIC_CALLS

// MODULE[js]: m1
// FILE: k.kt

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
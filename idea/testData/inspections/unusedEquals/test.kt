// WITH_RUNTIME

fun foo(a: Int, b: Int) {
    a == b // not used
    a == 1 // not used

    foo2 {
        a == b // not used
        a == b // used
    }

    foo3 {
        a == b // not used
        a == b // not used
    }

    val e = a == b // used
    a == b // not used
}

fun foo2(c: () -> Boolean) {

}

fun foo3(d: () -> Unit) {

}

fun foo4(a: Int): Boolean {
    return a == 1 // used
}

fun foo5(a: Int) = a == 1 // used

fun foo6(a: Int) {
    foo2 {
        fun foo7() {
            a == 1 // not used
        }
    }
}

fun foo7(a: Int) {
    run {
        a == b // used as return value of run
    }

    if (a == b) return // used
}

fun foo8(a: Int) {
    val eq = if (a > 1) a == 10 else a == -1 // used

    val eq2 = if (a > 1) {
        a == 10 // used
    }
    else {
        a == -1 // used
    }

    if (a > 1) a == 10 else a == -1 // both unused

    if (a == 10) {} else if (a == -1) {} // both used
}
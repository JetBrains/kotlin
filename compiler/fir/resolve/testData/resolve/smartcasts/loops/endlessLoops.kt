interface A {
    fun foo()
}

fun test_1(x: Any, b: Boolean) {
    while (true) {
        x as A
        if (b) {
            break
        }
    }
    x.foo()
}

fun test_2(x: Any, b: Boolean) {
    while (true) {
        if (b) {
            x as A
            break
        }
    }
    x.foo()
}

fun test_3(x: Any, b: Boolean) {
    while (true) {
        x as A
        if (b) {
            break
        }
        if (b) {
            break
        }
    }
    x.foo()
}

fun test_4(x: Any, b: Boolean) {
    while (true) {
        if (b) {
            x as A
            break
        }
        break
    }
    x.<!UNRESOLVED_REFERENCE!>foo<!>() // No smartcast
}

fun test_5(x: Any, b: Boolean) {
    do {
        if (b) {
            x as A
            break
        }
    } while (true)
    x.foo()
}

fun test_6(x: Any, b: Boolean) {
    do {
        x as A
        if (b) {
            break
        }
    } while (true)
    x.foo()
}

fun test_7(x: Any) {
    do {
        x as A
    } while (true)
    x.foo()
}

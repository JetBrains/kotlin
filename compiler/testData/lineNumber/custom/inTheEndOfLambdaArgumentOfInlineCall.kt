fun foo() {
    bar {
        nop()
        baz()
    }
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}

inline fun baz() {
    nop()
}

fun nop() {}

// 2 20 21 3 4 25 26 5 27 6 9 10 11 14 15 17
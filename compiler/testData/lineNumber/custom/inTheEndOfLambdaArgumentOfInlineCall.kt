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

// 2 20 21 3 4 22 23 5 24 6 9 10 11 14 15 17
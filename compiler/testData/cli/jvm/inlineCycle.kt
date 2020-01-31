inline fun a(q: () -> Unit) {
    b(q)

    // check that nested not recognized as cycle
    c {
        c {

        }
    }

    withDefaults()
}

inline fun withDefaults(x: Int = 1) = x * 2

inline fun b(p: () -> Unit) {
    p()
    a(p)
}

inline fun c(p: () -> Unit) {
    p()
}
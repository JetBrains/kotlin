inline fun a(l: () -> Unit) {
    b(l)

    //check that nested not recognized as cycle
    c {
        c {

        }
    }
}


inline fun b(p: () -> Unit) {
    p()
    a(p)
}

inline fun c(p: () -> Unit) {
    p()
}
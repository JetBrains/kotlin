fun a() {
    c {
        return
    }

    c {
        return@a
    }
}

inline fun c(p: () -> Unit) {
    p()
}
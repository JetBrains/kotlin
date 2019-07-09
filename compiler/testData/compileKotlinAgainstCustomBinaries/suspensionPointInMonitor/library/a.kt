val lock = Any()

inline fun inlineMe(c: () -> Unit) {
    synchronized(lock) {
        c()
    }
}

inline fun monitorInFinally(a: () -> Unit, b: () -> Unit) {
    try {
        a()
    } finally {
        synchronized(lock) {
            b()
        }
    }
}
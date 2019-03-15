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

inline fun withCrossinline(crossinline a: suspend () -> Unit): suspend () -> Unit {
    val c : suspend () -> Unit = {
        inlineMe {
            a()
        }
    }
    return c
}
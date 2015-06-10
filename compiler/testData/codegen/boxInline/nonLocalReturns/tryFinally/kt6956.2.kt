package test

inline fun baz(x: Int) {}

inline fun <T> foo(action: () -> T): T {
    baz(0)
    try {
        return action()
    } finally {
        baz(1)
    }
}


package test

inline fun foo(f: () -> Unit) {
    try {
        f()
    }
    finally {
        1
    }
}
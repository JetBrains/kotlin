package test

inline fun call(crossinline s: () -> String): String {
    return {
        s()
    }()
}
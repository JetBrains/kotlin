package test

inline fun call(s: () -> String): String {
    return s()
}
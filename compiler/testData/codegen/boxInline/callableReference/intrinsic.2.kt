package test

inline fun call(p: String, s: String.() -> Int): Int {
    return p.s()
}
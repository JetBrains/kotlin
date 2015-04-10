package test

inline fun call(p: Int, s: Int.(Int) -> Int): Int {
    return p.s(p)
}
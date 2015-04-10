package test

inline fun call(p: Int, s: (Int) -> Int): Int {
    return s(p)
}
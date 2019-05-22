fun box(): String {
    return if (call(10, Int::calc) == 5) "OK" else "fail"
}

val Int.calc: Int
    get() = this / 2

inline fun call(p: Int, s: (Int) -> Int): Int {
    return s(p)
}

// 0 NEW
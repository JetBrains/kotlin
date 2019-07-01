fun box(): String {
    return if (call(10, A()::calc) == 5) "OK" else "fail"
}

class A {
    fun calc(p: Int): Int {
        return p / 2
    }
}

inline fun call(p: Int, s: (Int) -> Int): Int {
    return s(p)
}

// 1 NEW A
// 1 NEW
fun box(): String {
    return if (call(A(10)::calc) == 5) "OK" else "fail"
}

class A(val p: Int) {
    val calc: Int
        get() = p / 2
}

inline fun call( s: () -> Int): Int {
    return s()
}

// 1 NEW A
// 1 NEW
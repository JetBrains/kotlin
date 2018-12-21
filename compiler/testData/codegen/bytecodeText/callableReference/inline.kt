// IGNORE_BACKEND: JVM_IR
fun box(): String {
    return if (call(10, ::calc) == 5) "OK" else "fail"
}

fun calc(p: Int): Int {
    return p / 2
}

inline fun call(p: Int, s: (Int) -> Int): Int {
    return s(p)
}

// 0 NEW
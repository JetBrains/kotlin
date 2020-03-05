fun foo(p: Int?): Boolean {
    return M<Int>(p)?.nulled() == 1
}

fun foo2(p: Int?): Boolean {
    return 1 == M<Int>(p)?.nulled()
}

class M<T: Any>(val z: T?) {
    fun nulled(): T? = z
}


fun box(): String {
    if (foo(null)) return "fail 1"
    if (!foo(1)) return "fail 2"

    if (foo2(null)) return "fail 1"
    if (!foo2(1)) return "fail 2"
    return "OK"
}
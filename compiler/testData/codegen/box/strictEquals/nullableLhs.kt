// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

class A(val n: Int) {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = n == other.n
}

fun box(): String {
    val x: A? = A(42)
    val y: Any? = A(42)
    if (x != null && x == y) {
        if (y.n != 42) return "FAIL 1"
    } else {
        return "FAIL 2"
    }
    val nullA: A? = null
    if (nullA == y) return "FAIL 3"
    if (nullA != null) return "FAIL 4"
    return "OK"
}

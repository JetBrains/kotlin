// LANGUAGE: +StrictEquals

class A(val n: Int) {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = n == other.n
}

fun box(): String {
    val a = A(42)
    val b: Any? = A(42)
    val c: Any? = A(1)

    if (!a.equals(b)) return "FAIL 1"
    if (a.equals(c)) return "FAIL 2"

    if (a.equals(b)) {
        if (b.n != 42) return "FAIL 3"
    } else {
        return "FAIL 4"
    }

    return "OK"
}

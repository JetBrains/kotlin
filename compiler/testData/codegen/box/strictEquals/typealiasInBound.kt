// LANGUAGE: +StrictEquals

class A(val n: Int) {
    override fun equals(@EqualityBound(AAlias::class) other: Any?): Boolean = n == other.n
}

typealias AAlias = A

fun box(): String {
    if (A(1) != A(1)) return "FAIL 1"
    if (A(1) == A(2)) return "FAIL 2"
    return "OK"
}

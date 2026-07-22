// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

class A(val n: Int) {
    override fun equals(@EqualityBound(AAlias::class) other: Any?): Boolean = n == other.n
}

typealias AAlias = A

fun box(): String {
    if (A(1) != A(1)) return "FAIL 1"
    if (A(1) == A(2)) return "FAIL 2"
    return "OK"
}

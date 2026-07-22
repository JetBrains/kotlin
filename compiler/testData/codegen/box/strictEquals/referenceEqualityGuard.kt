// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

var callCount = 0

class A(val n: Int) {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean {
        callCount++
        return n == other.n
    }
}

fun box(): String {
    val a = A(1)
    callCount = 0
    if (a != a) return "FAIL 1"
    if (callCount != 0) return "FAIL 2: === guard should have prevented body call, got $callCount"

    if (a != A(1)) return "FAIL 3"
    if (callCount != 1) return "FAIL 4: body should have been called once, got $callCount"
    return "OK"
}

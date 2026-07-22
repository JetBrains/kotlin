// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

open class Base(val n: Int) {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = n == other.n
}

class Child(n: Int) : Base(n)

fun box(): String {
    val c1 = Child(1)
    val c2 = Child(1)
    val b1 = Base(1)
    if (c1 != c2) return "FAIL 1"
    if (c1 != b1) return "FAIL 2"
    if (c1 == Child(2)) return "FAIL 3"
    return "OK"
}

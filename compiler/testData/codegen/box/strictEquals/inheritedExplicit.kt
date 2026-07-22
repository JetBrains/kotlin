// LANGUAGE: +StrictEquals

open class Base(val n: Int) {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = n == other.n
}

class Child(n: Int, val extra: String) : Base(n) {
    override fun equals(@EqualityBound(Child::class) other: Any?): Boolean =
        n == other.n && extra == other.extra
}

fun box(): String {
    if (Base(1) != Base(1)) return "FAIL 1"
    if (Child(1, "a") != Child(1, "a")) return "FAIL 2"
    if (Child(1, "a") == Child(1, "b")) return "FAIL 3"
    if (Base(1) != Child(1, "a")) return "FAIL 4"
    if (Child(1, "a") == Base(1)) return "FAIL 5"
    return "OK"
}

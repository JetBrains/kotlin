class X(val n: Int) {
    fun map(f: (Int) -> Int) = X(f(n))
}

fun box(): String {
    val s = (for (i in X(3)) yield i*2).n.toString()
    return if (s == "6") "OK" else "FAIL: $s"
}
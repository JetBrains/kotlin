class X(val n: Int) {
    fun map(f: (Int) -> Int) = X(f(n))
    fun flatMap(f: (Int) -> X) = f(n)
}

fun box(): String {
    val s = (for (i in X(3), j in X(i + 2)) yield j*j).n.toString()
    return if (s == "25") "OK" else "FAIL: $s"
}
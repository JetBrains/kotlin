class X<T>(val t: T) {
    fun map(f: (T) -> T) = X(f(t))
}

fun box(): String {
    val s = (for (i in X(X(3))) yield for (j in X(i.t + 1)) yield j*2).t.t.toString()
    return if (s == "8") "OK" else "FAIL: $s"
}
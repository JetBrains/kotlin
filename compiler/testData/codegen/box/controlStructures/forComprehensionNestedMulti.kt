class X<T>(val t: T) {
    fun map<U>(f: (T) -> U): X<U> = X(f(t))
    fun flatMap<U>(f: (T) -> X<U>): X<U> = f(t)
}

fun box(): String {
    val s = (for (i in X(3), j in X(i + 1)) yield for (k in X(i*j)) yield k*k).t.t.toString()
    return if (s == "144") "OK" else "FAIL: $s"
}
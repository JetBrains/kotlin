fun <A: Double, B: A> f(a: Double, b: B) = a == b

fun box(): String {
    if (f(0.1, 0.2)) return "FAIL"
    return "OK"
}

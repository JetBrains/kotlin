// IGNORE_BACKEND: NATIVE

fun equals1(a: Double, b: Double) = a.equals(b)

fun box(): String {
    if ((-0.0).equals(0.0)) return "fail 0"
    return "OK"
}

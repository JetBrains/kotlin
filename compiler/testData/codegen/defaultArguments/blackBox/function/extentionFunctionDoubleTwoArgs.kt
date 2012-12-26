fun Double.foo(a: Double = 1.0, b: Double = 1.0): Double {
    return a + b
}

fun box(): String  {
    if (1.0.foo() != 2.0) return "fail"
    if (1.0.foo(2.0, 2.0) != 4.0) return "fail"
    if (1.0.foo(a = 2.0) != 3.0) return "fail"
    if (1.0.foo(b = 2.0) != 3.0) return "fail"
    return "OK"
}
class A {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun box() : String {
    var (a, b) = A()
    a = b
    return if (a == 2 && b == 2) "OK" else "fail"
}

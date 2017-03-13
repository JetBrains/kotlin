class A {
}

operator fun A.component1() = 1
operator fun A.component2() = 2

fun box() : String {
    val (a, b) = A()
    return if (a == 1 && b == 2) "OK" else "fail"
}
class A {
}

fun A.component1() = 1
fun A.component2() = 2

fun box() : String {
    val (a, b) = A()
    return if (a == 1 && b == 2) "OK" else "fail"
}
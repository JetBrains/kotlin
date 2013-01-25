class A {
    fun component1() = 1
    fun component2() = 2

}

fun A.getA() = this

fun box() : String {
    val (a, b) = A().getA().getA()

    return if (a == 1 && b == 2) "OK" else "fail"
}

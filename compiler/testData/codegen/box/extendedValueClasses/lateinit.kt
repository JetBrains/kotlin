// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// WITH_STDLIB

value class A(val x: Int)

value class B(val x: Int, val y: Int)

lateinit var a0: A
lateinit var b0: B

fun box(): String {
    lateinit var a1: A
    lateinit var b1: B
    a0 = A(1)
    b0 = B(2, 3)
    a1 = A(4)
    b1 = B(5, 6)
    val actual = listOf(a0.x, b0.x, b0.y, a1.x, b1.x, b1.y)
    require(actual == (1..6).toList()) { actual.toString() }
    return "OK"
}

// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// WITH_STDLIB

value class A(val x: Int, val y: Int)

@JvmRecord
value class B(val x: Int, val y: Int)

fun box(): String {
    val a = A(1, 2)
    val b = B(1, 2)
    require(a.x == 1) { a.x.toString() }
    require(b.x == 1) { b.x.toString() }
    require(a.y == 2) { a.y.toString() }
    require(b.y == 2) { b.y.toString() }
    return "OK"
}

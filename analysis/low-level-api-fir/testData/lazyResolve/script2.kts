// RESOLVE_SCRIPT

fun f(p: Int?): Int {
    val fy = p!!
    val fz = p + 1
    return fz
}

fun consume(x: Int) {}

val x: Int? = 1

val y = x!!

val z = x + 1

fun foo() = x + 1

consume(x + 1)
x!!
consume(x + 1)

val zz = x + 1

class A {
    val cz = x + 1
    fun bar() = x + 1
}

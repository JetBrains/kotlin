package pack

data class B(val a: A, val n: Int)

class C {
    operator fun component1(): A = TODO()
    operator fun component2() = 0
}

fun f(b: B, c: C) {
    val (a, n) = b
    val (x, y, z) = a

    val (a1, n1) = c
    val (x1, y1, z1) = a1
}
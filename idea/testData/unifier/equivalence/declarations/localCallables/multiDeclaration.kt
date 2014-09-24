data class A(val a: Int, val b: String)

fun foo1() {
    <selection>val (a, b) = A(1, "2")</selection>
    val (u, v) = A(2, "2")
    val (x, y) = A(1, "2")
}

fun foo2() {
    fun A.component1(): Int = a + 1

    val (aa, bb) = A(1, "2")
    val (uu, vv) = A(2, "2")
    val (xx, yy) = A(1, "2")
}

fun foo3() {
    val (u, v) = A(2, "2")
    val (x, y) = A(1, "2")
}
import B

object A {
    val c = 1
    fun foo() = 4
}

fun box(): String {
    return if (B.a == 1 && B.b == 4) "OK" else "${B.a} ${B.b}"
}
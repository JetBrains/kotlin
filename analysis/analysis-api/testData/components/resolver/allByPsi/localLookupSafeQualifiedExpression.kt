data class A(val a: A?, val n: Int)

fun f(a: A) {
    val (a1, n1) = a
    val (a2, n2) =
        a?.a ?: return
    val (a3, n3) = a1 ?: return
}

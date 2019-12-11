// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val i: Int, val j: G)
data class B(val i: G, val j: G)


fun fa(a: A) {
    val (i, j) = a
    val i2 = a.component1()
    val j2 = a.component2()
}

fun fb(b: B) {
    val (i, j) = b
    val i2 = b.component1()
    val j2 = b.component2()
}
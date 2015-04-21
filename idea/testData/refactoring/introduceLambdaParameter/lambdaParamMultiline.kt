// WITH_DEFAULT_VALUE: false
fun foo(a: Int, s: String): Int {
    val t = a + 1 + 2
    return <selection>a
            .plus(1)
            .plus(2)</selection> - t
}

fun test() {
    foo(1, "2")
}
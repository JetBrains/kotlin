// WITH_DEFAULT_VALUE: false
fun foo(a: Int, s: String): Int {
    val t = (a + 1) * 2
    return <selection>1 + 2</selection> - t
}

fun test() {
    foo(1, "2")
}
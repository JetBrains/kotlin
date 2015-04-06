// WITH_DEFAULT_VALUE: false
fun foo(a: Int): Int {
    val b = (<selection>a + 1</selection>) * 2
    return a + 1
}

fun test() {
    foo(1)
}
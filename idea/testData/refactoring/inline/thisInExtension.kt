// WITH_RUNTIME
fun Int.foo(a: Int, b: Int): Int {
    val x = this * 2
    return with(a + b) { x + this }
}
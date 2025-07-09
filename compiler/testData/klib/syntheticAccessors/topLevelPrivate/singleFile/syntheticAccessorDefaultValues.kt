private fun foo(x: String = "OK", y: String = x) = y
internal inline fun bar() = foo()

fun box() : String {
    return bar()
}

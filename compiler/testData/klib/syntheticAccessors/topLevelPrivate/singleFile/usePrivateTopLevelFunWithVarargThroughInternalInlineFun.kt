private fun foo(vararg x: String, y: String = x[0]) = y
internal inline fun bar() = foo("OK")

fun box() : String {
    return bar()
}

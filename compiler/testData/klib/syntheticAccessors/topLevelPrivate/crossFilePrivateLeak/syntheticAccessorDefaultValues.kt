// FILE: A.kt
private fun foo(x: String = "OK", y: String = x) = y
internal inline fun bar() = foo()

// FILE: B.kt
fun box() : String {
    return bar()
}

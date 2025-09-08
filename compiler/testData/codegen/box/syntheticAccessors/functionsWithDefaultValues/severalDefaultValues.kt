// FILE: A.kt
private fun foo(x: String = "N", y: String = "O", z: String = "K") = x + y + z
internal inline fun bar() = foo("")

// FILE: B.kt
fun box() : String {
    return bar()
}
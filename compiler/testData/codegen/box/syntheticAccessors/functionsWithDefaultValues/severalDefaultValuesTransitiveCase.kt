// FILE: A.kt
private fun foo(x: String = "N", y: String = "O", z: String = "K") = x + y + z
private inline fun bar(x: String = "") = foo(x)
internal inline fun baz() = bar()

// FILE: B.kt
fun box() : String {
    return baz()
}
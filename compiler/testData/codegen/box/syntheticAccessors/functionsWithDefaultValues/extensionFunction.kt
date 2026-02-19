// FILE: A.kt
private fun String.foo(x: String = "K") = this + x
internal inline fun bar() = "O".foo()

// FILE: B.kt
fun box() : String {
    return bar()
}
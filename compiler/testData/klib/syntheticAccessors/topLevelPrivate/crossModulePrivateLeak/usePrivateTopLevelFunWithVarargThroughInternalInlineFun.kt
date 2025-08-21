// MODULE: lib
// FILE: A.kt
private fun foo(vararg x: String, y: String = x[0]) = y
internal inline fun bar() = foo("OK")

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return bar()
}

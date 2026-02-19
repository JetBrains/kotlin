// MODULE: lib
// FILE: A.kt
private fun foo(x: String = "OK") = x
internal inline fun bar() = foo()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return bar()
}
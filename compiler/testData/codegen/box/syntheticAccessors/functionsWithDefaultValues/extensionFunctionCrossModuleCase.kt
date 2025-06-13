// MODULE: lib
// FILE: A.kt
private fun String.foo(x: String = "K") = this + x
internal inline fun bar() = "O".foo()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return bar()
}
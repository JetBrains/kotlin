// MODULE: lib
// FILE: A.kt
private fun String.foo(x: String = "K") = this + x
private inline fun bar() = "O".foo()
internal inline fun baz() = bar()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return baz()
}
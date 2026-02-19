// MODULE: lib
// FILE: A.kt
private fun foo(x: String = "OK") = x
private inline fun bar() = foo()
internal inline fun baz() = bar()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return baz()
}
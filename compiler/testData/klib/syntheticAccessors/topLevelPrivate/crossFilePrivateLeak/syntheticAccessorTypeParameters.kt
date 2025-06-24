// FILE: A.kt
class Box<T>(val v: T)
private fun <T> foo(x: T, y: Box<T> = Box<T>(x)) = y.v
internal inline fun bar() = foo<String>("OK")

// FILE: B.kt
fun box() : String {
    return bar()
}

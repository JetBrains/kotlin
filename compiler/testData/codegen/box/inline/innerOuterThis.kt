// FILE: lib.kt
package foo

inline fun<T> with1(value: T, p: T.() -> Unit) = value.p()

// FILE: main.kt
package foo

class A(val expected: String) {
    val b = B()

    fun foo(): A {
        with1(b) {
            y = expected
        }
        return this
    }
}
class B() {
    var y = ""
}

fun box() = A("OK").foo().b.y
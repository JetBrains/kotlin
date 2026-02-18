// FILE: lib.kt
package foo

class X
class Y
class Z

inline fun <reified A, B, reified C> test(x: Any): String =
        when (x) {
            is A -> "A"
            is C -> "C"
            else -> "Unknown"
        }

// FILE: main.kt
package foo
import kotlin.test.*

fun box(): String {
    assertEquals("A", test<X, Y, Z>(X()))
    assertEquals("Unknown", test<X, Y, Z>(Y()))
    assertEquals("C", test<X, Y, Z>(Z()))

    return "OK"
}
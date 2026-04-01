// FILE: lib.kt
package foo

class A
class B

fun <T, R> apply(x: T, fn: T.()->R): R = x.fn()

inline fun <reified T, reified R> test(x: Any, y: Any): Boolean =
        x is T && apply(y) { this is R }

// FILE: main.kt
package foo
import kotlin.test.*

fun box(): String {
    val a = A()
    val b = B()

    assertEquals(true, test<A, B>(a, b), "test<A, B>(a, b)")
    assertEquals(false, test<A, B>(a, a), "test<A, B>(a, a)")
    assertEquals(false, test<A, B>(b, b), "test<A, B>(b, b)")
    assertEquals(false, test<A, B>(b, a), "test<A, B>(b, a)")

    return "OK"
}
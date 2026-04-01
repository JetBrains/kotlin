// FILE: lib.kt
package foo

// CHECK_NOT_CALLED: test

class A

inline fun <reified T> test(): String {
    val a: Any = A()

    return if (a is T) "A" else "Unknown"
}

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_NOT_CALLED: test
fun box(): String {
    assertEquals("A", test<A>())
    assertEquals("Unknown", test<String>())

    return "OK"
}
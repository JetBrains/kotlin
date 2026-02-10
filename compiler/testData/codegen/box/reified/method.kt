// FILE: lib.kt
package foo

class A(val x: Any? = null) {
    inline fun <reified T> test() = x is T
}

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_NOT_CALLED: test

class B

fun box(): String {
    assertEquals(true, A(A()).test<A>(), "A(A()).test<A>()")
    assertEquals(false, A(B()).test<A>(), "A(B()).test<A>()")

    return "OK"
}
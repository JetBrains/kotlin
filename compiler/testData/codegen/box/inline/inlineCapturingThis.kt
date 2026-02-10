// FILE: lib.kt
package foo

inline fun block(p: () -> Unit) = p()

// FILE: main.kt
package foo
import kotlin.test.*

class A(val x: Int) {
    fun test(): Int {
        var result: Int = 0
        block {
            result = x
        }
        return result
    }
}

fun box(): String {
    assertEquals(23, A(23).test())

    return "OK"
}
// FILE: lib.kt
package foo

class A {
    var x = 23
}

inline fun bar(value: Int, a: A): Int {
    a.x = 42
    return value
}

// FILE: main.kt
package foo
import kotlin.test.*

fun box(): String {
    val a = A()
    assertEquals(23, bar(a.x, a))
    return "OK"
}
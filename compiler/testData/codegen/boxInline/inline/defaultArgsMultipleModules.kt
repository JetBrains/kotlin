// WITH_STDLIB
// FILE: lib.kt

package a

inline fun foo(x: Int, y: Int = 117) = x + y

// FILE: main.kt

import a.*
import kotlin.test.*

fun box(): String {
    assertEquals(122, foo(5))
    assertEquals(47, foo(5, 42))
    return "OK"
}
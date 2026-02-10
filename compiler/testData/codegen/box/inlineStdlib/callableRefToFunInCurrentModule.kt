// KJS_WITH_FULL_RUNTIME
// FILE: lib.kt
package foo

internal inline fun even(x: Int) = x % 2 == 0

// FILE: main.kt
package foo
import kotlin.test.*

internal fun test(a: List<Int>) = a.filter(::even)

fun box(): String {
    assertEquals(listOf(2, 4), test(listOf(1, 2, 3, 4)))

    return "OK"
}
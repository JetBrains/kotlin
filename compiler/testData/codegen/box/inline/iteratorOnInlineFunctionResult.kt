// FILE: lib.kt
package foo
import kotlin.test.*

inline fun bar(f: () -> Int): Array<Int> = arrayOf(f())

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box(): String {
    val iterator = bar { 23 }.iterator()
    assertEquals(true, iterator.hasNext())
    assertEquals(23, iterator.next())
    assertEquals(false, iterator.hasNext())

    return "OK"
}
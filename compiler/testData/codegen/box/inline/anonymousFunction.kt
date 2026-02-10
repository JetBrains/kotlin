// FILE: lib.kt
package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun <T> block(f: () -> T): T = f()

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_BREAKS_COUNT: function=test count=0
// CHECK_LABELS_COUNT: function=test name=$l$block count=0
fun test() = block(fun(): Int { return 23 })

fun box(): String {
    assertEquals(23, test())
    return "OK"
}

// FILE: lib.kt
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: test except=Unit_getInstance

// A copy of stdlib run function.
// Copied to not to depend on run implementation.
// It's important, that the body is just `return fn()`.
internal inline fun <T> evaluate(fn: ()->T): T = fn()

// FILE: main.kt
import kotlin.test.*
// CHECK_CONTAINS_NO_CALLS: test except=Unit_getInstance
// CHECK_BREAKS_COUNT: function=test count=0
// CHECK_LABELS_COUNT: function=test name=$l$block count=0
internal fun test(n: Int): Int {
    return evaluate {
        var i = n
        var sum = 0

        while (i > 0) {
            sum += i
            i--
        }

        sum
    }
}

fun box(): String {
    assertEquals(6, test(3))
    assertEquals(0, test(0))
    assertEquals(0, test(-1))

    return "OK"
}
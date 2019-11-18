// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf(1, 2, 3, 4)
    var sum = 0
    var index = arr.size - 1
    for (i in arr.reversed().reversed()) {
        // reversed() returns a new List with elements in reversed order.
        // Modifying the original array should have no effect on the iteration subject.
        arr[index--] = 0
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)

    return "OK"
}
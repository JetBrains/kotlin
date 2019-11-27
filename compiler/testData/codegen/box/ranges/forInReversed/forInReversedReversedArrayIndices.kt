// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf(1, 1, 1, 1)
    var sum = 0
    for (i in arr.indices.reversed().reversed()) {
        sum = sum * 10 + i + arr[i]
    }
    assertEquals(1234, sum)

    return "OK"
}
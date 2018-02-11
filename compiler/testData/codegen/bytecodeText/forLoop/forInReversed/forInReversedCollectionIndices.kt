// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val xs = listOf(1, 1, 1, 1)
    var sum = 0
    for (i in xs.indices.reversed()) {
        sum = sum * 10 + i + xs[i]
    }
    assertEquals(4321, sum)

    return "OK"
}

// 0 reversed
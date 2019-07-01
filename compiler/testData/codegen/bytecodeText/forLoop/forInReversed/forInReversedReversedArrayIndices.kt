import kotlin.test.*

fun box(): String {
    val arr = intArrayOf(1, 1, 1, 1)
    var sum = 0
    for (i in arr.indices.reversed().reversed()) {
        sum = sum * 10 + i + arr[i]
    }
    assertEquals(4321, sum)

    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

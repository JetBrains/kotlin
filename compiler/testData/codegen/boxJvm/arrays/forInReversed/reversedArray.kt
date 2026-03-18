// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    testReversed()
    testReversedArray()
    return "OK"
}

fun testReversed() {
    val arr = intArrayOf(6, 7, 8, 9)
    var result = ""
    for (i in arr.reversed()) {
        arr[0] = 0
        result += i
    }
    assertEquals("9876", result)
}

fun testReversedArray() {
    val arr = intArrayOf(6, 7, 8, 9)
    var result = ""
    for (i in arr.reversedArray()) {
        arr[0] = 0
        result += i
    }
    assertEquals("9876", result)
}

// CHECK_BYTECODE_TEXT
// 0 java/util/List.iterator
// 2 IINC
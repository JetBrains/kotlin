// WITH_STDLIB

import kotlin.test.*

fun testContinue7() {
    var x = 0
    fun inc() = ++x
    for (i in 0..1) {
        for (j in inc() downTo continue) {}
    }
    assertEquals(2, x)
}

fun testContinue8() {
    var x = 0
    fun inc() = ++x
    for (i in 0..1) {
        for (j in continue downTo inc()) {}
    }
    assertEquals(0, x)
}

fun box(): String {
    testContinue7()
    testContinue8()
    return "OK"
}
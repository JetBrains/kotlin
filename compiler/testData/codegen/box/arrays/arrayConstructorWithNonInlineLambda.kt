// WITH_STDLIB

import kotlin.test.assertEquals


val size = 10

fun box(): String {

    val intArray = IntArray(size)

    val array = Array(size) { i -> { intArray[i]++ } }

    for (i in intArray) {
        assertEquals(0, i)
    }

    for (a in array) {
        a()
    }

    for (i in intArray) {
        assertEquals(1, i)
    }

    return "OK"
}

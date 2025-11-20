// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 4

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array[i - 1] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array1[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size + 10) {
            array[i] = 6
        }
    }
    return "OK"
}

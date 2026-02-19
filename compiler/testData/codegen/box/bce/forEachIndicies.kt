// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 0 }
    val array1 = Array(3) { 0 }
    var j = 4

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.indices) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.indices) {
            array[i + 1] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.indices) {
            array1[i] = 6
        }
    }
    return "OK"
}

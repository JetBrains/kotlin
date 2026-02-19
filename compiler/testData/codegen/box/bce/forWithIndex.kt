// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var j = 8

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[index + 1] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[value] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in (0..array.size + 30 step 2).withIndex()) {
            array[index] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in (0..array.size).withIndex()) {
            array[value] = 8
        }
    }
    return "OK"
}

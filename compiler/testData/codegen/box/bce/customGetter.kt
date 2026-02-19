// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

var needSmallArray = true

val array: Array<Int> = arrayOf(1)
    get() = if (needSmallArray) field else arrayOf(1, 2, 3)

fun box(): String {
    val a = array
    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (index in 0 until array.size) {
            a[index] = 6
        }
    }

    return "OK"
}

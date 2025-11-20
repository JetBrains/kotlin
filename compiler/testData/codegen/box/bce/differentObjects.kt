// KT-66100: AssertionError: Expected an exception of class IndexOutOfBoundsException to be thrown, but was completed successfully.
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

class First(initArray: Array<Int>) {
    val array = initArray
}

class Second(initArray: Array<Int>){
    val first = First(initArray)
}

class Third(initArray: Array<Int>) {
    val second = Second(initArray)
}

fun box(): String {
    val a = Third(arrayOf(1, 2, 3, 4, 5))
    val b = Third(arrayOf(1, 2))

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..a.second.first.array.size-1) {
            b.second.first.array[i] = 6
        }
    }
    return "OK"
}

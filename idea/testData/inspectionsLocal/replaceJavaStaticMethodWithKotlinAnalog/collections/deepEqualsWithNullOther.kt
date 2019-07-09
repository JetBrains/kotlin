// WITH_RUNTIME
// PROBLEM: none
import java.util.Arrays

fun test() {
    val a = arrayOf(1, 2, 3)
    val b: Array<*>? = null
    val result = Arrays.<caret>deepEquals(a, b)
}
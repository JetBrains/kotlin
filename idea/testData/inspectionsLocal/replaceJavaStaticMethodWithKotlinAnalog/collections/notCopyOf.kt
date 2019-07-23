// WITH_RUNTIME
// PROBLEM: none
import java.util.Arrays

fun test() {
    val array = arrayOf(1, 2, 3)
    val result = Arrays.<caret>copyOf(array, 3, Array<Double>::class.java)
}
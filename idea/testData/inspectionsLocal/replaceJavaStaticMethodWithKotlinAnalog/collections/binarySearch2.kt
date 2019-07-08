// WITH_RUNTIME
import java.util.Arrays

fun test() {
    val array = arrayOf(1, 2, 3)
    val from = 1
    val to = 4
    val result = Arrays.<caret>binarySearch(array, from, to, 3)
}
// WITH_RUNTIME
import java.util.Arrays

fun test() {
    val array = intArrayOf(1, 2, 3)
    val result = Arrays.<caret>copyOf(array, 3)
}
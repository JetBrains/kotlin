// WITH_RUNTIME
import java.util.Arrays

fun test() {
    val array = arrayOf(1, 2, 3)
    val result = Arrays.<caret>copyOfRange(array, 3, 5)
}
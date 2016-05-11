// WITH_RUNTIME
import java.util.ArrayList

fun foo(array: IntArray): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (i in array) {
        if (i % 3 == 0) {
            result.add(i)
        }
    }
    return result
}

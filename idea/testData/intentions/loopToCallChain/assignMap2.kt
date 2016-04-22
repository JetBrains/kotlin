// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.map{}'"
import java.util.ArrayList

fun foo(list: List<String>): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (s in list) {
        if (s.length > 0) {
            val h = s.hashCode()
            result.add(h)
        }
    }
    return result
}
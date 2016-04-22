// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}'"
import java.util.*

fun foo(list: List<String>): List<String> {
    val result = ArrayList<String>()
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index) {
            result.add(s)
        }
    }
    return result
}
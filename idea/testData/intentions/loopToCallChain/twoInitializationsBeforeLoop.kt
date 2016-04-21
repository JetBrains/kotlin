// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.map{}'"
import java.util.*

fun foo(list: List<String>): List<Int> {
    val result = ArrayList<Int>()
    var i = 0
    <caret>for (s in list) {
        if (s.length > i) {
            result.add(s.length)
        }
        i++
    }
    return result
}
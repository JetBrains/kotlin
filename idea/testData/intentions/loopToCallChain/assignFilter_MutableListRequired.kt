// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.toMutableList()'"
import java.util.ArrayList

fun foo(list: List<String>): MutableList<String> {
    val result = ArrayList<String>()
    <caret>for (s in list) {
        if (s.length > 0) {
            result.add(s)
        }
    }
    return result
}
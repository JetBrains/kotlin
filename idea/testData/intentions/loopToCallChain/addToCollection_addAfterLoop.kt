// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'toMutableList()'"
import java.util.ArrayList

fun foo(map: Map<Int, String>): List<String> {
    val result = ArrayList<String>()
    <caret>for (s in map.values) {
        result.add(s)
    }
    result.add("")
    return result
}
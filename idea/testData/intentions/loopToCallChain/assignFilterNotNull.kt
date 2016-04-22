// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNull()'"
import java.util.ArrayList

fun foo(list: List<String?>): List<String> {
    val result = ArrayList<String>()
    <caret>for (s in list) {
        if (s != null) {
            result.add(s)
        }
    }
    return result
}
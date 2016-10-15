// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+='"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(map: Map<Int, String>) {
    val result: ArrayList<String> = ArrayList()
    <caret>for (s in map.values) {
        result.add(s)
    }
}
// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'toSet()'"
// IS_APPLICABLE_2: false
import java.util.HashSet

fun foo(map: Map<Int, String>): Collection<String> {
    val result = HashSet<String>()
    <caret>for (s in map.values) {
        result.add(s)
    }
    return result
}
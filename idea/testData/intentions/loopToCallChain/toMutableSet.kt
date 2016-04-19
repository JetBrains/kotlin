// WITH_RUNTIME
import java.util.HashSet

fun foo(map: Map<Int, String>): MutableCollection<String> {
    val result = HashSet<String>()
    <caret>for (s in map.values) {
        result.add(s)
    }
    return result
}
// WITH_RUNTIME
import java.util.HashSet

fun foo(map: Map<Int, String>): Collection<Int> {
    val result = HashSet<Int>()
    <caret>for (s in map.values) {
        result.add(s.length)
    }
    return result
}
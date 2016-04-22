// WITH_RUNTIME
// IS_APPLICABLE: false
import java.util.ArrayList

fun foo(list: List<String>): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (s in list) {
        if (s.length > result.size) {
            result.add(s.hashCode())
        }
    }
    return result
}
// WITH_RUNTIME
import java.util.*

fun foo(list: List<String>): ArrayList<String> {
    return run {
        val result = ArrayList<String>()
        <caret>for (s in list) {
            if (s.length > 0) {
                result.add(s)
            }
        }
        result
    }
}
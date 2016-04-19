// WITH_RUNTIME
import java.util.ArrayList

fun foo(list: List<String>, p: Int): ArrayList<String> {
    return if (p > 0) {
        val result = ArrayList<String>()
        <caret>for (s in list) {
            if (s.length > 0) {
                result.add(s)
            }
        }
        result
    }
    else {
        ArrayList()
    }
}
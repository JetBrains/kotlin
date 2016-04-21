// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNullTo()'"
import java.util.ArrayList

fun foo(list: List<String?>) {
    val target = ArrayList<String>(1000)
    <caret>for (s in list) {
        if (s != null) {
            target.add(s)
        }
    }
}
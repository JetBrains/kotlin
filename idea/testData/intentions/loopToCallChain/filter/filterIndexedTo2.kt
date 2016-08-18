// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexedTo(){}'"
// IS_APPLICABLE_2: false
import java.util.ArrayList

fun foo(list: List<String>): List<String> {
    val result = ArrayList<String>(1000)
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index)
            result.add(s)
    }
    return result
}
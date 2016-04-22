// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}'"
import java.util.*

fun foo(list: List<String>) {
    val result = ArrayList<Int>()

    bar()

    <caret>for (s in list) {
        result.add(s.length)
    }
}

fun bar(){}
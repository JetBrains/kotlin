// WITH_RUNTIME
// IS_APPLICABLE: FALSE
import java.util.LinkedList

fun Int.withIndices(): List<Pair<Int, Int>> = LinkedList<Pair<Int, Int>>()

fun foo(s: Int) {
    for ((index<caret>, a) in s.withIndices()) {

    }
}
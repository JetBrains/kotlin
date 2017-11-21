// WITH_RUNTIME
import java.util.Collections

fun test() {
    val mutableList = mutableListOf(1, 2)
    <caret>Collections.sort(mutableList, { a, b -> a.compareTo(b) })
}

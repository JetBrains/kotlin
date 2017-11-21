// WITH_RUNTIME
// PROBLEM: none
import java.util.Collections

fun test() {
    val list = listOf(1, 2)
    <caret>Collections.sort(list)
}

// WITH_RUNTIME
import java.util.ArrayList

fun foo(list: List<String>): List<String> {
    val target = ArrayList<String>(100)
    <caret>for (s in list) {
        for (line in s.lines()) {
            target.add(line)
        }
    }
    return target
}
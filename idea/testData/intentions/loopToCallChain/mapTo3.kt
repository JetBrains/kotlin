// WITH_RUNTIME
import java.util.ArrayList

fun foo(list: List<String>): List<Int> {
    val target = ArrayList<Int>(100)
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s.hashCode())
    }
    return target
}
